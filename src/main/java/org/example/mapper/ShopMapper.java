package org.example.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.example.entity.Shop;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public interface ShopMapper extends BaseMapper<Shop> {
    IPage<Shop> queryClosestShops(IPage<?> page, @Param("type") Long type,
                                  @Param("x") Double x, @Param("y") Double y);
}
