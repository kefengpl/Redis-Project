package org.example.dto;

import lombok.Data;

/**
 * @Description 登录用户数据传输
 * */
@Data
public class LoginFormDTO {
    private String phone;
    private String code;
    private String password;
}
