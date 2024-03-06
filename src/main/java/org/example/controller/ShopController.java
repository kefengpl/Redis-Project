package org.example.controller;

import org.example.dto.Result;
import org.example.entity.Shop;
import org.example.mapper.ShopMapper;
import org.example.service.IShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author 3590
 * @Date 2024/2/20 22:29
 * @Description
 */
@RestController
@RequestMapping("shop")
public class ShopController {
    @Autowired
    IShopService shopService;
    @Autowired
    ShopMapper shopMapper;

    /**
     * 根据 id 查询 shop 信息，可以使用 redis 作为 MySQL 的 Cache
     * */
    @GetMapping("{id}")
    public Result queryShopById(@PathVariable Long id) {
        return shopService.queryShopById(id);
    }

    /**
     * 展示商铺，并根据距离由小到大排序
     * */
    @GetMapping("of/type")
    public Result showShopList(Long typeId, Long current, Double x, Double y) {
        return shopService.showShopList(typeId, current, x, y);
    }

    /**
     * 更新使用 put
     * */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        return shopService.updateShop(shop);
    }

    /**
     * 根据名称分页查询商铺信息
     */
    @GetMapping("/of/name")
    public Result queryShopName(@RequestParam(value = "name", required = false) String name,
                                @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return shopService.queryShopName(name, current);
    }

    @GetMapping("test")
    public Result test(Long id) {
        Shop shop = shopMapper.selectById(id);
        System.out.println(shop);
        return Result.ok(shop);
    }

}
