package org.example.utils;

import io.netty.util.internal.StringUtil;
import org.example.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.example.utils.RedisConstants.*;
import static org.example.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * @Author 3590
 * @Date 2024/2/23 23:50
 * @Description 一些和 redis 缓存相关的工具
 */
@Component
public class RedisCacheUtil {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    SetnxLock setnxLock;
    /** 创建单线程池 */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(1);

    /***
     * @param id 查询的 id
     * @param cacheKeyPrefix 缓存key的前缀
     * @param queryFunction 比如 mapper.selectById
     * @param <R> 返回类型，即查询结果的 bean
     * 此函数的作用在于，在解决缓存击穿问题时，在逻辑过期的情况下异步更新缓存
     */
    public <R, ID> void asynchronousRefreshRedis(ID id, String cacheKeyPrefix, Function<ID, R> queryFunction) {
        String cacheKey = cacheKeyPrefix + id;
        // 具体的，查询数据库，将其重新添加到 redis 中
        Runnable refreshRedis = () -> {
            try {
                addWrapperDataToCache(HelperUtil.dataWrapper(queryFunction.apply(id)), cacheKey);
            } finally {
                setnxLock.releaseLock(); // 更新完成后释放锁
            }
        };
        // 优化：将这里转为线程池。当然一个线程，频繁创建和销毁也行...但强烈建议使用线程池
        CACHE_REBUILD_EXECUTOR.execute(refreshRedis);
    }

    /***
     * @param redisWrapperData 带有逻辑过期字段(wrapper)的数据
     * @param cacheKey 缓存 key 的名称
     * 将数据库查询的结果(带有逻辑过期字段)，添加到 cache 中
     */
    public <T> void addWrapperDataToCache(RedisWrapperData<T> redisWrapperData, String cacheKey) {
        if (redisWrapperData == null) {
            // 一种缓存穿透的解决方案：redis 中缓存空对象
            // 注意：redis 是懒创建的，只有存在具体键值对，才会创建 hash，传入空 map 不会创建该键值对
            // 解决方案：设置一个 _placeholder 字段，表示一个空的 hashmap 即可
            redisTemplate.opsForValue().set(cacheKey, NULL_PLACEHOLDER);
            redisTemplate.expire(cacheKey, CACHE_NULL_TTL, TimeUnit.MINUTES); // 提示：为了解决缓存穿透，依然需要给 null 设置空
        } else {
            // 重建缓存的过程，存储为 JSON 格式
            redisTemplate.opsForValue().set(cacheKey, HelperUtil.beanToJson(redisWrapperData));
        }
    }

    /***
     * 在缓存中查询记录，如果缓存未命中，返回null；如果缓存命中且是空键值对，则返回错误；
     * 如果缓存命中且不空，则检查是否逻辑过期，如果逻辑过期且获得了分布式锁，就异步更新，仍返回旧数据；如果无法获得锁，也返回旧数据；如果没有过期，直接返回结果
     * @param id 查询店铺的 id
     * @param failMessage 查询到空 map 返回失败信息
     */
    public <ID, R> Result cacheQueryById(ID id, String cacheKeyPrefix, String failMessage, Function<ID, R> queryFunction) {
        String cacheKey = cacheKeyPrefix + id;
        String jsonData = redisTemplate.opsForValue().get(cacheKey);

        // 缓存命中，但也有可能是空值(_placeholder == true)。如果是空，则需要返回 “店铺不存在”。
        if (!StringUtil.isNullOrEmpty(jsonData)) {
            // 空记录的情况
            if (jsonData.equals(NULL_PLACEHOLDER)) {
                return Result.fail(failMessage);
            }

            // 不是空记录，取出数据
            RedisWrapperData<R> wrapperData = HelperUtil.jsonToBean(jsonData, RedisWrapperData.class); // java 泛型本身的缺陷

            // 新开一个线程，让他来更新逻辑过期的数据。本线程返回旧版数据即可。提示：锁依然需要使用分布式锁。
            if (HelperUtil.isExpired(wrapperData.getExpireTime(), CACHE_SHOP_TTL, ChronoUnit.MINUTES)
                    && setnxLock.getLock()) {
                // 注意：可能需要对过期双重检查。假设 线程A：检查过期(已经过期) ---- ---- 获得锁
                //                        异步更新线程：             更新缓存完成 释放锁
                if (HelperUtil.isExpired(wrapperData.getExpireTime(), CACHE_SHOP_TTL, ChronoUnit.MINUTES))
                    //如果缓存已经逻辑过期且获得了分布式锁，则新开线程异步执行更新数据的操作。
                    asynchronousRefreshRedis(id, cacheKeyPrefix, queryFunction);
            }

            return Result.ok(wrapperData.getData());

        }
        return null;
    }
}
