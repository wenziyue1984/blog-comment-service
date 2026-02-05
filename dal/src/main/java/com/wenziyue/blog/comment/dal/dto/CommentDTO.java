package com.wenziyue.blog.comment.dal.dto;

import com.wenziyue.blog.comment.common.enums.CommentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wenziyue
 */
@Data
public class CommentDTO implements Serializable {

    private static final long serialVersionUID = 3930567962248554184L;

    @Schema(description = "评论id", example = "1", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long id;

    @Schema(description = "文章id", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long articleId;

    @Schema(description = "评论内容", example = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(min = 1, max = 500, message = "评论长度1~500个字符")
    private String content;

    @Schema(description = "父评论id，一级评论的话不需要", example = "1", requiredMode = Schema.RequiredMode.AUTO)
    private Long parentId;

    @Schema(description = "评论状态", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private CommentStatusEnum status;

    @Schema(description = "一级评论id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long levelOneId;

    @Schema(description = "子评论", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<CommentDTO> children;

    @Schema(description = "子评论总数", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer childrenTotalCount;

    @Schema(description = "是否有更多子评论", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean hasMoreChildren;

    @Schema(description = "用户id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long userId;

    @Schema(description = "用户名称", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String userName;

    @Schema(description = "用户头像url", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String userAvatarUrl;

    @Schema(description = "被回复用户id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long replyUserId;

    @Schema(description = "被回复用户名称", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String replyUserName;

    @Schema(description = "点赞数", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer likeCount;

    @Schema(description = "评论时间", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime createTime;

}
