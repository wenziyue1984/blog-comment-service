package com.wenziyue.blog.comment.infra.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

/**
 * @author wenziyue
 */
@Data
@RequiredArgsConstructor
public class UserInfoFeignDTO implements Serializable {

    private static final long serialVersionUID = 4803675955483890466L;

    private Long id;

    private String name;

    private String avatarUrl;

    private String email;

    private String phone;

    private String bio;

    private Integer status;

    private Integer role;
}
