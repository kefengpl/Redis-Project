package org.example.controller;

import org.example.dto.Result;
import org.example.entity.Voucher;
import org.example.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author 3590
 * @Date 2024/2/28 1:31
 * @Description
 */
@RestController
@RequestMapping("voucher")
public class VoucherController {
    @Autowired
    private IVoucherService voucherService;

    /**
     * 存储秒杀券的信息
     * 不仅将数据保存到 tb_voucher，还要保存到 tb_seckill_voucher
     * */
    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * 存储普通券
     */
    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    @GetMapping("list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
        return voucherService.queryVoucherOfShop(shopId);
    }
}
