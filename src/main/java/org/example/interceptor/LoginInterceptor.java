package org.example.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.dto.UserDTO;
import org.example.entity.User;
import org.example.utils.UserHolder;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import static org.example.utils.UserHolder.USER_KEY;

/**
 * @Author 3590
 * @Date 2024/2/19 20:22
 * @Description 设置登录拦截器，验证用户的登录。注意结束后删除用户信息，防止 ThreadLocal 内存泄漏
 */
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession session = request.getSession();
        UserDTO userDTO = (UserDTO) session.getAttribute(USER_KEY); // 获取用户
        if (userDTO == null) {
            response.setStatus(401);
            return false; // 未登录，返回401状态码，并拦截请求
        }
        UserHolder.saveUser(userDTO); // 存入ThreadLocal中
        return true; // 放行
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                         @Nullable Exception ex) throws Exception {
        // 请求结束之后，释放用户信息
        UserHolder.removeUser();
    }


}
