package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.entity.Voucher;
import org.example.mapper.VoucherMapper;
import org.example.service.IVoucherService;
import org.springframework.stereotype.Service;


@Service
public class VoucherServiceImpl extends ServiceImpl<VoucherMapper, Voucher> implements IVoucherService {

}
