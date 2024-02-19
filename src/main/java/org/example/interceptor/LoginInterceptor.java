package org.example.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;


/**
 * @Author 3590
 * @Date 2024/2/19 20:22
 * @Description 设置登录拦截器，验证用户的登录。注意结束后删除用户信息，防止 ThreadLocal 内存泄漏
 * 注意：一些技巧，由于 LoginInterceptor 没有被放入 IOC 容器，所以无法自动装配 stringRedisTemplate
 * 应该在构造这个实例的时候想办法注入 stringRedisTemplate，即 在它的配置类 中注入 stringRedisTemplate
 *
 * 更新：该拦截器只负责登录拦截，不再进行其它操作
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
