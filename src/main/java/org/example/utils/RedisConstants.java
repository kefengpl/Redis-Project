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
    public static final String STREAM_QUEUE_NAME = "stream.orders";
    public static final String BLOKE_LIKED_KEY = "blog:liked:";
    public static final String USER_FOLLOWING_KEY = "user:following:";
    /** 基于 redis 实现用户收件箱的 key 的前缀 */
    public static final String RECEIVE_MAIL_KEY = "user:receive:";
    /** FEED 滚动分页时，一页显示的推文条数 */
    public static final Integer FEED_PAGE_SIZE = 3;
    public static final String SIGN_KEY_PREFIX = "sign:";
    // redis 优惠券秒杀 lua 脚本对象
    public static final DefaultRedisScript<Long> VOUCHER_SECKILL_SCRIPT;
    // redis 检测某个用户是否点赞过
    public static final DefaultRedisScript<Long> BLOG_LIKED_SCRIPT;

    static {
        VOUCHER_SECKILL_SCRIPT = new DefaultRedisScript<>();
        VOUCHER_SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        VOUCHER_SECKILL_SCRIPT.setResultType(Long.class);

        BLOG_LIKED_SCRIPT = new DefaultRedisScript<>();
        BLOG_LIKED_SCRIPT.setLocation(new ClassPathResource("blog-like.lua"));
        BLOG_LIKED_SCRIPT.setResultType(Long.class);
    }
}
