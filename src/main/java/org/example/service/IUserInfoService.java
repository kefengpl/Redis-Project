package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.dto.Result;
import org.example.entity.UserInfo;

public interface IUserInfoService extends IService<UserInfo> {

    Result getUserInfo(Long userId);
}
