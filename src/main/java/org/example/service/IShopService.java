package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.Result;
import org.example.entity.Shop;

public interface IShopService extends IService<Shop> {

    Result queryShopById(Long id);

    Result showShopList(Long typeId, Long current, Double x, Double y);

    Result updateShop(Shop shop);
}
