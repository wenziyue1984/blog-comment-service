package com.wenziyue.blog.comment.dal.dto;

import com.wenziyue.blog.comment.dal.entity.NotifyOutboxEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author wenziyue
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotifyOutboxMqDTO implements Serializable {

    private static final long serialVersionUID = 4568713053382297941L;

    private Long commentId;

    private Long userId;

    private Long receiverId;

    public NotifyOutboxMqDTO(NotifyOutboxEntity entity) {
        this.commentId = entity.getCommentId();
        this.userId = entity.getUserId();
        this.receiverId = entity.getRecipientUserId();
    }
}
