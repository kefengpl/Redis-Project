package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.ShopType;

import java.util.List;


public interface IShopTypeService extends IService<ShopType> {
    List<ShopType> queryShopTypeList();
}
