package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.ShopType;
import org.example.mapper.ShopTypeMapper;
import org.example.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    ShopTypeMapper shopTypeMapper;

    @Override
    public List<ShopType> queryShopTypeList() {
        LambdaQueryWrapper<ShopType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ShopType::getSort);
        return shopTypeMapper.selectList(wrapper);
    }
}
