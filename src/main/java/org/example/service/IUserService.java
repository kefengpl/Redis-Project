package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.LoginFormDTO;
import org.example.dto.Result;
import org.example.entity.User;

import jakarta.servlet.http.HttpSession;

public interface IUserService extends IService<User> {
    public Result sendCode(String phone, HttpSession session);

    Result login(String phone, String verifyCode, HttpSession session);
}
