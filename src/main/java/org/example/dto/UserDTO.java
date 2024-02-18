package org.example.dto;

import lombok.Data;

/**
 * @Description 用户数据传输 (Data Transfer Object)
 * */
@Data
public class UserDTO {
    private Long id;
    private String nickName;
    private String icon;
}
