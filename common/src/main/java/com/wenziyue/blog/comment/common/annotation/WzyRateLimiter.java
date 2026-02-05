package com.wenziyue.blog.comment.common.annotation;

import java.lang.annotation.*;

/**
 * @author wenziyue
 */
@Target({ElementType.METHOD}) //表示这个注解可以加在类（Controller）或方法上。
@Retention(RetentionPolicy.RUNTIME) //表示这个注解在运行时生效，aop可以反射获取。
@Documented
public @interface WzyRateLimiter {
    /**
     * 限流的 key，比如可以使用 userId、IP 等。
     */
    String key();

    /**
     * 时间窗口（单位：毫秒）
     */
    long window();

    /**
     * 时间窗口内允许的请求次数
     */
    int maxCount();

    /**
     * 触发限流时提示的信息
     */
    String message() default "操作太频繁，请稍后再试";
}
