package org.example.interceptor;

import cn.hutool.core.bean.BeanUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dto.UserDTO;
import org.example.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.example.utils.RedisConstants.LOGIN_USER_KEY;
import static org.example.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * @Author 3590
 * @Date 2024/2/19 23:59
 * @Description 它放行所有请求，主要用于刷新和保存
 */
public class RefreshTokenInterceptor implements HandlerInterceptor {
    StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String token = request.getHeader("authorization");
        String userKey = LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(userKey);// 获取所有条目
        if (userMap.isEmpty()) return true; // 相信后人的智慧(第二个拦截器检查登录情况)
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        UserHolder.saveUser(userDTO); // 存入ThreadLocal中
        // 补充：刷新 token 的有效期，即：我们希望 token 能够像 session 一样，只要用户访问一次，token 有效期就恢复为 30 分钟
        // 当用户一直访问，那么 token 就会持续有效[刷新有效期]
        stringRedisTemplate.expire(userKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return true; // 放行
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                @Nullable Exception ex) throws Exception {
        // 请求结束之后，释放用户信息
        UserHolder.removeUser();
    }

}
