package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.Result;
import org.example.entity.Voucher;

public interface IVoucherService extends IService<Voucher> {

    void addSeckillVoucher(Voucher voucher);

    Result queryVoucherOfShop(Long shopId);
}
