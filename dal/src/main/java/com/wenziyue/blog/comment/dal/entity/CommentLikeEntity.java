package com.wenziyue.blog.comment.dal.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author wenziyue
 */
@Data
@NoArgsConstructor
@SuperBuilder
@TableName("TB_WZY_BLOG_COMMENT_LIKE")
public class CommentLikeEntity {

    @TableField("comment_id")
    private Long commentId;

    @TableField("user_id")
    private Long userId;

}
