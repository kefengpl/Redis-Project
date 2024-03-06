package org.example.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.dto.Result;
import org.example.dto.UserDTO;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.example.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.example.utils.RedisConstants.*;
import static org.example.utils.RegexUtils.isPhoneNotInvalid;
import static org.example.utils.SystemConstants.*;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    UserMapper userMapper;

    // 提示：不设置序列化的情况下，application.yml 会使得 SpringBoot 自动装配这个东西
    // StringRedisTemplate 可以使得所有元素以 <string, string> 的形式存储
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone) {
        if (isPhoneNotInvalid(phone)) {
            return Result.fail("手机号码格式错误");
        }
        String verifyCode = RandomUtil.randomNumbers(6); // 生成六位验证码
        // 更新：原来使用 session 存储验证码信息，现在直接存入 redis，key 是 phone(加上业务前缀)
        // 此外，给 key 设置有效区，防止空间被挤满，比如设置有效期 2 分钟
        stringRedisTemplate.opsForValue()
                .set(LOGIN_CODE_KEY + phone, verifyCode, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.debug("generate verify code: " + verifyCode); // // 发送验证码(模拟)
        return Result.ok(verifyCode);
    }

    @Override
    public Result login(String phone, String verifyCode) {
        if (isPhoneNotInvalid(phone)) {
            return Result.fail("手机号码格式错误");
        }
        String cachedCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
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
        // 为了便于存储 UserDTO 的所有属性，可以考虑使用反射。
        String token = saveUserToRedis(new UserDTO(user.getId(), user.getNickName(), user.getIcon()));
        return Result.ok(token); // 将 token 返回给客户端
    }

    public String getSignKey() {
        Long userId = UserHolder.getUser().getId();
        LocalDateTime now = LocalDateTime.now();
        int month = now.getMonth().getValue();
        int year = now.getYear();
        int day = now.getDayOfMonth();
        return SIGN_KEY_PREFIX + month + ":" + year + ":" + userId;
    }

    /**
     * 实现用户签到
     */
    @Override
    public Result sign() {
        LocalDateTime now = LocalDateTime.now();
        int day = now.getDayOfMonth();
        String signKey = getSignKey();
        // 偏移映射，每个月的几号，就直接把 几 - 1 这个比特位设置为 1
        stringRedisTemplate.opsForValue().setBit(signKey, day - 1, true);
        return Result.ok();
    }

    /**
     * 使用动态规划的思想统计连续签到次数
     * dp[i] 表示以 i 为结尾的连续1的数组最大长度
     * dp[i] = dp[i - 1] + 1 if nums[i] == 1
     * dp[i] = 0 if nums[i] == 0
     */
    @Override
    public Result signCount() {
        LocalDateTime now = LocalDateTime.now();
        int day = now.getDayOfMonth(); // 数组长度
        boolean[] signArray = new boolean[day];
        String signKey = getSignKey();
        for (int i = 0; i < signArray.length; ++i) {
            // 从第一天开始获取签到情况
            Boolean bit = stringRedisTemplate.opsForValue().getBit(signKey, i);
            signArray[i] = bit != null && bit;
        }
        int[] dp = new int[day];
        dp[0] = signArray[0] ? 1 : 0;
        int result = dp[0];
        for (int i = 1; i < dp.length; ++i) {
            if (signArray[i]) {
                dp[i] = dp[i - 1] + 1;
            } else {
                dp[i] = 0;
            }
            result = Math.max(result, dp[i]);
        }
        return Result.ok(result);
    }

    /**
     * 为了使得维护方便，使用反射来获取 userDTO 所有属性并存入数据库
     * @return 生成的token
     * */
    private String saveUserToRedis(UserDTO userDTO) {
        // 使用 token 作为 key，在 redis 中存储用户数据
        String token = RandomUtil.randomString(16);
        Class<? extends UserDTO> clazz = userDTO.getClass();
        Field[] fields = clazz.getDeclaredFields();
        String key = LOGIN_USER_KEY + token;
        Map<String, String> beanMap = new HashMap<>();
        for (Field field : fields) {
            field.setAccessible(true);
            String attribute = field.getName();
            try {
                // 反复多次将每个键值对存储会带来性能缺陷，因此可以使用 Map 先存好数据，然后 putAll
                // 提示：存储用户数据时，为了便于区分 key 的性质，需要加上前缀，于是 key 的效果就是：login:token:J88sWZMhdEXT7XtU
                beanMap.put(attribute, field.get(userDTO).toString());
            } catch (IllegalAccessException e) {
                log.debug(e.getMessage());
            }
        }
        stringRedisTemplate.opsForHash().putAll(key, beanMap);
        stringRedisTemplate.expire(key, LOGIN_USER_TTL, TimeUnit.MINUTES); // 有效期 30 分钟
        return token;
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
