package com.wenziyue.blog.comment.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wenziyue.blog.comment.common.enums.CommentDepthEnum;
import com.wenziyue.blog.comment.common.enums.CommentStatusEnum;
import com.wenziyue.mybatisplus.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author wenziyue
 */
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("TB_WZY_BLOG_COMMENT")
public class CommentEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("content")
    private String content;

    @TableField("user_id")
    private Long userId;

    @TableField("article_id")
    private Long articleId;

    @TableField("author_id")
    private Long authorId;

    @TableField("parent_id")
    private Long parentId;

    @TableField("reply_user_id")
    private Long replyUserId;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("read_status")
    private Boolean readStatus;

    @TableField("status")
    private CommentStatusEnum status;

    @TableField("depth")
    private CommentDepthEnum depth;

    @TableField("level_one_id")
    private Long levelOneId;

}
