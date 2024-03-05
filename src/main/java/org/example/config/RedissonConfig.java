package org.example.config;

import org.example.utils.RedisConnection;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author 3590
 * @Date 2024/3/4 23:07
 * @Description
 */
@Configuration
public class RedissonConfig {
    @Autowired
    RedisConnection redisConnection;

    @Bean
    public RedissonClient redissonClient(RedisConnection redisConnection) {
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("121773");
        return Redisson.create(config);
    }
}
