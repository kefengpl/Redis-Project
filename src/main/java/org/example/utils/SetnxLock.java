package org.example.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static org.example.utils.RedisConstants.*;
import static org.example.utils.SystemConstants.POLLING_INTERVAL;

/**
 * @Author 3590
 * @Date 2024/2/23 14:47
 * @Description 基于 redis 的 setnx 实现的分布式锁
 * setnx lock 1 : set the value of a key, only if the key does not exist
 * Redis没有直接的wait和notify机制。在分布式环境中，等待通常通过轮询锁状态来实现
 */
@Component
public class SetnxLock {
    @Autowired
    StringRedisTemplate redisTemplate;

    public boolean getLock() {
        return BooleanUtil.isTrue(redisTemplate.opsForValue().
                setIfAbsent(LOCK_SHOP_KEY, "1", LOCK_SHOP_TTL, TimeUnit.MINUTES));
    }

    /***
     * 轮询获取锁，直到得到锁为止。一个阻塞方法
     * */
    public void tryLock() {
        while (!getLock()) {
            try {
                Thread.sleep(POLLING_INTERVAL);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void releaseLock() {
        redisTemplate.delete(LOCK_SHOP_KEY);
    }
}
