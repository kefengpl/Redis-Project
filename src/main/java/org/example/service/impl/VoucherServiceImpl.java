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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.example.utils.RedisConstants.SECKILL_STOCK_KEY;


@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {
    @Autowired
    VoucherMapper voucherMapper;
    @Autowired
    SeckillVoucherMapper seckillVoucherMapper;
    @Autowired
    StringRedisTemplate redisTemplate;

    /**
     * 存储秒杀券到 MySQL 的同时，还要把券id和库存信息存储到 redis
     * @note 先写数据库，再写入缓存
     */
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
        // 写入 redis，秒杀优惠券的 id --> 库存
        // TTL 设置成永久，非必要不删除
        redisTemplate.opsForValue().set(SECKILL_STOCK_KEY + voucher.getId(),
                String.valueOf(voucher.getStock()));
    }

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        return Result.ok(voucherMapper.queryAllTypeofVouchers(shopId));
    }
}
