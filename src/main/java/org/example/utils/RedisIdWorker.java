package org.example.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @Author 3590
 * @Date 2024/2/28 0:48
 * @Description redis用于生成全局唯一的id
 * 1 bit 符号位，31 bit 时间戳，32 bit 序列号
 */
@Component
public class RedisIdWorker {
    private static final long BEGIN_TIMESTAMP = 1640995200;
    private static final int TIME_STAMP_MOV = 32;
    @Autowired
    private StringRedisTemplate redisTemplate;
    public long nextId(String keyPrefix) {
        // 1. 生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp = nowSecond - BEGIN_TIMESTAMP;

        // 2. 生成序列号，以每日存储
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd")); // format 20200918，一天一个key
        // 理论上不会出现空值
        long count = redisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);// 这个key对应的值会从0开始自增。
        return (timeStamp << TIME_STAMP_MOV) | count;
    }
}
