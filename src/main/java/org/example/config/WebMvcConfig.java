package org.example.config;

import org.example.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author 3590
 * @Date 2024/2/19 20:26
 * @Description 路径中的 ** 指的是通配符。我们需要放行(无需登录的功能)一些接口
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
            "/user/code", "/user/login", "/blog/hot", "/shop/**", "/shop-type/**", "/voucher/**", "/upload/**"
        );
    }
}
