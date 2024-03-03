package org.example.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author 3590
 * @Date 2024/2/23 16:26
 * @Description 给实体类添加逻辑过期字段 LocalDateTime expireTime 后的数据类型
 */
@Data
public class RedisWrapperData <T> {
    LocalDateTime expireTime;
    T data;
}
