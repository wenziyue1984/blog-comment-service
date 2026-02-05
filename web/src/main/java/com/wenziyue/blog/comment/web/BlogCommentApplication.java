package com.wenziyue.blog.comment.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author wenziyue
 */
@EnableAsync
@EnableScheduling
@MapperScan("com.wenziyue.blog.comment.dal.mapper")
@ComponentScan("com.wenziyue")
@SpringBootApplication(scanBasePackages = {"com.wenziyue"})
@EnableFeignClients(basePackages = "com.wenziyue.blog.comment")
public class BlogCommentApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(BlogCommentApplication.class, args);
    }
}
