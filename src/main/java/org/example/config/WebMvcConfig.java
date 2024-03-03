package org.example.config;

import org.example.interceptor.LoginInterceptor;
import org.example.interceptor.RefreshTokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author 3590
 * @Date 2024/2/19 20:26
 * @Description 路径中的 ** 指的是通配符。我们需要放行(无需登录的功能)一些接口
 * 提示：多个拦截器，有两种解决办法：如果 order 默认都是0，则先添加的 拦截器先拦截
 * 另一种方式，把 order 进行修改，人为指定拦截器执行的先后顺序，order 越小，越先执行
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    StringRedisTemplate stringRedisTemplate; // 由于 配置类 也是 component，所以支持自动装配

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
            "/user/code", "/user/login", "/blog/hot", "/shop/**", "/shop-type/**", "/voucher/**", "/upload/**", "/test/**"
        ).order(1);
    }
}
