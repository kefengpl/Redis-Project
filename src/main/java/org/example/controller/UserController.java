package org.example.controller;

import org.example.dto.Result;
import org.example.service.IUserInfoService;
import org.example.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.util.Map;


/**
 * @Author 3590
 * @Date 2024/2/18 23:06
 * @Description
 */
@RestController
@RequestMapping("user")
public class UserController {
    @Autowired
    IUserService userService;
    @Autowired
    IUserInfoService userInfoService;

    /**
     * 前端界面用户输入手机号，然后点击发送验证码后的效果
     * 需要做的是：验证收集号码，然后如果格式正确，就需要生成验证码，并返回验证码，且保存在本地的Session里面！
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 处理用户登录请求，注意由于传入的是JSON，需要注解RequestBody，此外，
     * 这里不创建实体对象的时候，只能用 map 这种东西接值
     * 优化：为了传输 phone + code，我们有DTO类，这样可以有效避免使用十分容易出错的 map + 属性值
     * */
    @PostMapping("login")
    public Result login(@RequestBody Map<String, String> map, HttpSession session) {
        String phone = map.get("phone");
        String verifyCode = map.get("code");
        return userService.login(phone, verifyCode, session);
    }
}
