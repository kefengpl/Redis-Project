package org.example.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.dto.Result;
import org.example.entity.Shop;
import org.example.mapper.ShopMapper;
import org.example.service.IShopService;

import org.example.utils.HelperUtil;
import org.example.utils.SetnxLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.example.utils.RedisConstants.*;
import static org.example.utils.SystemConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    ShopMapper shopMapper;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    SetnxLock setnxLock;

    /**
     * Redis 优化：它作为 MySQL 的 Cache。
     * 根据 id 查询商铺时，如果缓存未命中，则查询数据库，将结果写入缓存，并设置超时时间
     * 使用简单的空键值对方法，解决缓存穿透问题
     * -----------------------------
     * 使用 redis 的 setnx 来构建一个分布式锁。对查询数据库的操作加锁
     * */
    @Override
    public Result queryShopById(Long id) {
        String cacheKey = CACHE_SHOP_KEY + id;
        String failMessage = "id = " + id + " 的商户不存在";

        Result cacheHitResult = cacheQueryShopById(id, failMessage);
        if (cacheHitResult != null) return cacheHitResult; // 缓存命中

        // 缓存不命中：则需要去数据库查询
        // 对查询过程加锁
        try {
            setnxLock.tryLock();
            // 其它线程获得锁后重复查询一次，如果缓存命中，就结束该函数
            cacheHitResult = cacheQueryShopById(id, failMessage);
            if (cacheHitResult != null) return cacheHitResult; // 缓存命中

            // 将数据添加到数据库
            Shop shop = shopMapper.selectById(id);
            addShopToCache(shop, cacheKey);
            // 注意：执行顺序：①计算 return的值；②执行 finally 代码块 ③ 返回值
            return shop == null ? Result.fail(failMessage) : Result.ok(shop);
        } finally {
            setnxLock.releaseLock();
        }
    }

    /***
     * @param id 查询店铺的 id
     * @param failMessage 查询到空 map 返回失败信息
     * @return Result，如果缓存命中，Result就是返回查询到的信息；未命中，则返回 null
     */
    private Result cacheQueryShopById(Long id, String failMessage) {
        String cacheKey = CACHE_SHOP_KEY + id;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cacheKey);
        // 缓存命中，但也有可能是空值(_placeholder == true)。如果是空，则需要返回 “店铺不存在”。
        if (!entries.isEmpty()) {
            if (entries.containsKey(NULL_PLACEHOLDER)) { // 空 map 的情况
                return Result.fail(failMessage);
            } else {
                return Result.ok(BeanUtil.fillBeanWithMap(entries, new Shop(), false));
            }
        }
        return null;
    }

    /***
     * @param shop 可能是空
     * 将数据库查询的结果添加到 cache 中
     */
    private void addShopToCache(Shop shop, String cacheKey) {
        if (shop == null) {
            // 一种缓存穿透的解决方案：redis 中缓存空对象
            // 注意：redis 是懒创建的，只有存在具体键值对，才会创建 hash，传入空 map 不会创建该键值对
            // 解决方案：设置一个 _placeholder 字段，表示一个空的 hashmap 即可
            redisTemplate.opsForHash().put(cacheKey, NULL_PLACEHOLDER, "true");
        } else {
            // 重建缓存的过程
            Map<String, String> shopMap = HelperUtil.beanToStringMap(cacheKey, shop);
            redisTemplate.opsForHash().putAll(cacheKey, shopMap);
        }
        redisTemplate.expire(cacheKey, shop == null ? CACHE_NULL_TTL : CACHE_SHOP_TTL, TimeUnit.MINUTES); // 有效时长：30min
    }

    @Override
    public Result showShopList(Long typeId, Long current, Double x, Double y) {
        IPage<Shop> page = new Page<>(current, DEFAULT_PAGE_SIZE);
        shopMapper.queryClosestShops(page, typeId, x, y);
        List<Shop> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 更新策略：先更新数据库，再删除缓存，以保证线程安全。此外，该操作应该是原子的
     * */
    @Override
    @Transactional // 直接加上这个事务即可
    public Result updateShop(Shop shop) {
        if (shop.getId() == null) return Result.fail("店铺 id 不能为空");
        shopMapper.updateById(shop); // 更新数据库
        String cacheKey = CACHE_SHOP_KEY + shop.getId();
        redisTemplate.delete(cacheKey); // 删除缓存
        return Result.ok();
    }


}
