package org.example.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @Author 3590
 * @Date 2024/3/4 21:27
 * @Description
 */
public class SimpleRedisLock implements ILock {
    StringRedisTemplate redisTemplate;
    String name; // the name of the lock
    private static final String LOCK_PREFIX = "lock:";
    /** 用 uuid 标识唯一线程 */
    private static final String THREAD_ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    /** Long 泛型表示 脚本返回的数值类型是 long */
    private static final DefaultRedisScript<Long> REDIS_UNLOCK_SCRIPT;

    static {
        REDIS_UNLOCK_SCRIPT = new DefaultRedisScript<>();
        REDIS_UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        REDIS_UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(StringRedisTemplate redisTemplate, String name) {
        this.redisTemplate = redisTemplate;
        this.name = name;
    }

    private String getThreadId() {
        return THREAD_ID_PREFIX + Thread.currentThread().getId();
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 将数值设置为当前线程标识
        String threadId = getThreadId();
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 调用 lua 脚本以实现两个命令的原子性
     */
    @Override
    public void unlock() {
        // 第二个参数：锁的 key，第三个参数 arg是线程标识
        // 这个脚本取代了下面的下面函数的两个命令。注意 keys 是一个集合，所以需要生成单元素集合
        redisTemplate.execute(REDIS_UNLOCK_SCRIPT,
                Collections.singletonList(LOCK_PREFIX + name), getThreadId());
    }

    /*@Override
    public void unlock() {
        // 不能释放别人的锁，只能释放自己的锁！
        String threadId = getThreadId();
        if (threadId.equals(redisTemplate.opsForValue().get(LOCK_PREFIX + name))) {
            redisTemplate.delete(LOCK_PREFIX + name); // 只释放自己的锁
        }
    }*/
}
