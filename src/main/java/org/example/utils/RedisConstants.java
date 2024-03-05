package org.example.utils;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public class RedisConstants {
    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;
    public static final String LOGIN_USER_KEY = "login:token:";
    public static final Long LOGIN_USER_TTL = 30L;

    public static final Long CACHE_NULL_TTL = 5L;

    public static final Long CACHE_SHOP_TTL = 10L;
    public static final String CACHE_SHOP_KEY = "cache:shop:";
    public static final String CACHE_SHOP_TYPE_KEY = "cache:shop-type";
    public static final String NULL_PLACEHOLDER = "null-placeholder";
    public static final String LOCK_SHOP_KEY = "lock:shop:";
    public static final Long LOCK_SHOP_TTL = 10L;

    public static final String SECKILL_STOCK_KEY = "seckill:stock:";
    public static final String SECKILL_ORDER_KEY = "seckill:order:";
    // redis 优惠券秒杀 lua 脚本对象
    public static final DefaultRedisScript<Long> VOUCHER_SECKILL_SCRIPT;

    static {
        VOUCHER_SECKILL_SCRIPT = new DefaultRedisScript<>();
        VOUCHER_SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        VOUCHER_SECKILL_SCRIPT.setResultType(Long.class);
    }
}
