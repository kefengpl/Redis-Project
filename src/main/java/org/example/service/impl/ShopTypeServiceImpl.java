package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.dto.Result;
import org.example.entity.ShopType;
import org.example.mapper.ShopTypeMapper;
import org.example.service.IShopTypeService;
import org.example.utils.HelperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.example.utils.RedisConstants.*;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    ShopTypeMapper shopTypeMapper;
    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * 更新：使用 Redis 实现店铺类型缓存。这里使用 list 和 json 来实现
     * */
    @Override
    public Result queryShopTypeList() {
        List<String> stringList = redisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, -1);// begin to end
        if (!stringList.isEmpty()) {
            return Result.ok(HelperUtil.collectionElemToBean(stringList, ShopType.class));
        }
        // 缓存未命中，则需要存储数据结构
        LambdaQueryWrapper<ShopType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ShopType::getSort);
        List<ShopType> shopTypeList = shopTypeMapper.selectList(wrapper);
        redisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_KEY, HelperUtil.collectionElemToJson(shopTypeList));
        redisTemplate.expire(CACHE_SHOP_TYPE_KEY, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shopTypeMapper.selectList(wrapper));
    }
}
