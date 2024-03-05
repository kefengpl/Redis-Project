package org.example.controller;

import org.example.dto.Result;
import org.example.service.IVoucherOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author 3590
 * @Date 2024/3/3 21:27
 * @Description
 */
@RequestMapping("voucher-order")
@RestController
public class VoucherOrderController {
    @Autowired
    IVoucherOrderService voucherOrderService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.redisSeckillVoucher(voucherId);
    }
}
