package org.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.dto.Result;
import org.example.entity.UserInfo;
import org.example.mapper.UserInfoMapper;
import org.example.service.IUserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {
    @Autowired
    UserInfoMapper userInfoMapper;

    @Override
    public Result getUserInfo(Long userId) {
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) return Result.ok();
        userInfo.setCreateTime(null);
        userInfo.setUpdateTime(null);
        return Result.ok(userInfo);
    }
}
