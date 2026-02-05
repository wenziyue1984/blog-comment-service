package com.wenziyue.blog.comment.dal.dto;

import com.wenziyue.mybatisplus.page.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wenziyue
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentPageDTO extends PageRequest {

    private static final long serialVersionUID = -6499761314141342360L;

    @Schema(description = "文章id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long articleId;

    @Schema(description = "一级评论id,只在请求二级评论时使用", example = "1", requiredMode = Schema.RequiredMode.AUTO)
    private Long oneLevelCommentId;

    @Schema(description = "排序方式, 0时间正序，1时间倒序，2点赞数高到低", example = "1", requiredMode = Schema.RequiredMode.AUTO)
    private Integer sort;

}
