package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.dto.Result;
import org.example.entity.SeckillVoucher;
import org.example.entity.Voucher;
import org.example.mapper.SeckillVoucherMapper;
import org.example.mapper.VoucherMapper;
import org.example.service.IVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {
    @Autowired
    VoucherMapper voucherMapper;
    @Autowired
    SeckillVoucherMapper seckillVoucherMapper;
    @Override
    @Transactional
    public void addSeckillVoucher(Voucher voucher) {
        voucherMapper.insert(voucher);
        SeckillVoucher seckillVoucher = new SeckillVoucher();
        seckillVoucher.setVoucherId(voucher.getId());
        seckillVoucher.setStock(voucher.getStock());
        seckillVoucher.setBeginTime(voucher.getBeginTime());
        seckillVoucher.setEndTime(voucher.getEndTime());
        seckillVoucherMapper.insert(seckillVoucher);
    }

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        return Result.ok(voucherMapper.queryAllTypeofVouchers(shopId));
    }
}
