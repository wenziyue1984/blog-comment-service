package com.wenziyue.blog.comment.infra.dto;


import lombok.Data;

import java.io.Serializable;

/**
 * @author wenziyue
 */
@Data
public class TagDTO implements Serializable {

    private static final long serialVersionUID = 585939726783483601L;

    private Long id;

    private String name;

    private Integer status;
}
