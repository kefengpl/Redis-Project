package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.Result;
import org.example.entity.User;

public interface IUserService extends IService<User> {
    public Result sendCode(String phone);

    Result login(String phone, String verifyCode);

    Result sign();

    Result signCount();
}
