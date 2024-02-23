package org.example.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author 3590
 * @Date 2024/2/21 13:59
 * @Description 辅助注解，用于替代 @JacksonIgnore
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface GsonIgnore {
}
