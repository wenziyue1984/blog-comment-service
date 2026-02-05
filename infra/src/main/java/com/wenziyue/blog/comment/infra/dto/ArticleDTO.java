package com.wenziyue.blog.comment.infra.dto;

import com.wenziyue.blog.comment.infra.enums.ArticleStatusEnum;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author wenziyue
 */
@Data
@RequiredArgsConstructor
public class ArticleDTO implements Serializable {

    private static final long serialVersionUID = -6512176632744749728L;

    private Long id;

    private Long userId;

    private String title;

    private String content;

    private String summary;

    private String coverUrl;

    private List<TagDTO> tagList;

    private Integer viewCount;

    private Integer likeCount;

    private String slug;

    private Boolean isTop;

    private Integer sort;

    private ArticleStatusEnum status;
}