package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.Result;
import org.example.entity.SeckillVoucher;
import org.example.entity.VoucherOrder;

/**
 * @Author 3590
 * @Date 2024/3/3 21:29
 * @Description
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    Result seckillVoucher(Long voucherId);

    Result redisSeckillVoucher(Long voucherId);

    Long makeOrder(SeckillVoucher seckillVoucher, Long userId);
    Long makeOrder(VoucherOrder voucherOrder);
}
