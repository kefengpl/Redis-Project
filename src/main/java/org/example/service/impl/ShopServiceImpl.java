package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import org.example.dto.Result;
import org.example.entity.Shop;
import org.example.mapper.ShopMapper;
import org.example.service.IShopService;

import org.example.utils.HelperUtil;
import org.example.utils.RedisCacheUtil;
import org.example.utils.RedisWrapperData;
import org.example.utils.SetnxLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    @Autowired
    RedisCacheUtil redisCacheUtil;

    /**
     * Redis 优化：它作为 MySQL 的 Cache。
     * 根据 id 查询商铺时，如果缓存未命中，则查询数据库，将结果写入缓存，并设置超时时间
     * 使用简单的空键值对方法，解决缓存穿透问题
     * -----------------------------
     * 使用 redis 的 setnx 来构建一个分布式锁。对查询数据库的操作加锁
     * 使用逻辑过期的解决办法。注意：逻辑过期的情况下，使用空键值对解决缓存穿透也是可行的，当然，这会导致空键值对被反复查询。
     * [空键值对也逻辑过期即可]
     */
    @Override
    public Result queryShopById(Long id) {
        String cacheKey = CACHE_SHOP_KEY + id;
        String failMessage = "id = " + id + " 的商户不存在";

        Result cacheHitResult = redisCacheUtil.cacheQueryById(id, CACHE_SHOP_KEY, failMessage, shopMapper::selectById);
        if (cacheHitResult != null) {
            return cacheHitResult; // 缓存命中
        }

        // 缓存不命中：则需要去数据库查询
        // 对查询过程加锁。注意：该代码块只会执行一次，就是最开始未命中的情况；当然，空键值对可能需要反复运行下面的代码。
        try {
            setnxLock.tryLock(); // 注意：这里的等待机制可能是性能瓶颈
            // 其它线程获得锁后重复查询一次，如果缓存命中，就结束该函数
            cacheHitResult = redisCacheUtil.cacheQueryById(id, CACHE_SHOP_KEY, failMessage, shopMapper::selectById);
            if (cacheHitResult != null) {
                return cacheHitResult; // 缓存命中，这也会运行 finally 代码块
            }

            // 将数据添加到数据库。先写数据库，再写缓存
            RedisWrapperData<Shop> wrapperData = HelperUtil.dataWrapper(shopMapper.selectById(id));
            redisCacheUtil.addWrapperDataToCache(wrapperData, cacheKey);
            // 注意：执行顺序：①计算 return的值；②执行 finally 代码块 ③ 返回值
            return wrapperData == null ? Result.fail(failMessage) : Result.ok(wrapperData.getData());
        } finally {
            setnxLock.releaseLock();
        }
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
     * @bug 可能由于逻辑删除带来一些问题；如果对一致性要求不高，则完全无需删除缓存。
     */
    @Override
    @Transactional // 直接加上这个事务即可
    public Result updateShop(Shop shop) {
        if (shop.getId() == null) return Result.fail("店铺 id 不能为空");
        shopMapper.updateById(shop); // 更新数据库
        String cacheKey = CACHE_SHOP_KEY + shop.getId();
        redisTemplate.delete(cacheKey); // 删除缓存
        return Result.ok();
    }

    @Override
    public Result queryShopName(String name, Integer current) {
        IPage<Shop> page = new Page<>(current, DEFAULT_PAGE_SIZE);
        LambdaQueryWrapper<Shop> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(!StringUtil.isNullOrEmpty(name), Shop::getName, name);
        shopMapper.selectPage(page, queryWrapper);
        return Result.ok(page.getRecords());
    }
}
