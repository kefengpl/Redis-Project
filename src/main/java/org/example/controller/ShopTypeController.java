package org.example.controller;

import org.example.dto.Result;
import org.example.entity.ShopType;
import org.example.service.IShopTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author 3590
 * @Date 2024/1/29 2:09
 * @Description
 */
@RestController
@RequestMapping("shop-type")
public class ShopTypeController {
    @Autowired
    IShopTypeService shopTypeService;

    @GetMapping("list")
    public Result shopTypeList() {
        return shopTypeService.queryShopTypeList();
    }
}
