package org.example.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author 3590
 * @Date 2024/2/23 16:26
 * @Description
 */
@Data
public class RedisWrapperData <T> {
    LocalDateTime expireTime;
    T data;
}
