package com.wenziyue.blog.comment.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wenziyue.blog.comment.common.enums.NotifyOutboxStatusEnum;
import com.wenziyue.mybatisplus.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * @author wenziyue
 */

@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("TB_WZY_BLOG_NOTIFY_OUTBOX")
public class NotifyOutboxEntity extends BaseEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("comment_id")
    private Long commentId;

    @TableField("user_id")
    private Long userId;

    @TableField("recipient_user_id")
    private Long recipientUserId;

    @TableField("status")
    private NotifyOutboxStatusEnum status;

    @TableField("owner")
    private String owner;

    @TableField("claim_time")
    private LocalDateTime claimTime;

}
