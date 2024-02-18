package org.example.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.dto.Result;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

import static org.example.utils.RegexUtils.isPhoneNotInvalid;
import static org.example.utils.SystemConstants.*;


@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    UserMapper userMapper;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (isPhoneNotInvalid(phone)) {
            return Result.fail("手机号码格式错误");
        }
        String verifyCode = RandomUtil.randomNumbers(6); // 生成六位验证码
        session.setAttribute("verifyCode", verifyCode); // 验证码储存到 session 中
        log.debug("generate verify code: " + verifyCode); // // 发送验证码(模拟)
        return Result.ok(verifyCode);
    }

    @Override
    public Result login(String phone, String verifyCode, HttpSession session) {
        if (isPhoneNotInvalid(phone)) {
            return Result.fail("手机号码格式错误");
        }
        String cachedCode = (String) session.getAttribute(VERIFY_CODE_NAME);
        if (cachedCode == null || !cachedCode.equals(verifyCode))
            return Result.fail("验证码校验失败");
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        User user = null;
        if (!userMapper.exists(wrapper)) {
            user = createUserWithPhone(phone);
            userMapper.insert(user);
        } else {
            user = userMapper.selectOne(wrapper);
        }
        session.setAttribute("userId", user); // 保存整个用户
        return Result.ok();
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX +
                RandomUtil.randomString(USER_NICK_NAME_LENGTH));
        // 提示：创建和更新时间似乎会自动维护，无需手动处理
        return user;
    }


}
