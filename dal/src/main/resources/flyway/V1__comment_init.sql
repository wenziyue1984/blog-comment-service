-- 文章评论表
-- 索引分别如下：
-- idx_one_lv_time 一级评论分页用，按照时间排序(虽然where条件中有deleted=0，但是绝大多数数据都是deleted都是0，所以这个字段对于范围缩小并不明显，而且还会增大联合索引体积，所以没必要加deleted)
-- idx_one_lv_like 一级评论分页用，按照点赞数排序
-- idx_two_lv_time 二级评论分页查询用(按照时间排序)
-- idx_levelOne_two_levelTwo 一级评论下两条二级评论用
-- idx_levelOne_deleted 统计子评论数量用
CREATE TABLE IF NOT EXISTS TB_WZY_BLOG_COMMENT
(
    `id`            BIGINT       NOT NULL PRIMARY KEY COMMENT '主键ID',
    `version`       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除 0-正常 1-删除',
    `create_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `content`       VARCHAR(500) NOT NULL COMMENT '评论内容',
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `article_id`    BIGINT       NOT NULL COMMENT '文章评论',
    `author_id`     BIGINT       NOT NULL COMMENT '作者ID',
    `parent_id`     BIGINT                DEFAULT NULL COMMENT '父评论id，null为一级评论',
    `reply_user_id` BIGINT       NOT NULL COMMENT '被回复的用户ID',
    `like_count`    INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `read_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0-正常 1-隐藏',
    `depth`         INT          NOT NULL COMMENT '深度，0-一级评论,1-二级评论',
    `level_one_id`  BIGINT                DEFAULT NULL COMMENT '一级评论id，二级评论专属字段，null为本身就是一级评论',
    INDEX idx_one_lv_time (article_id, level_one_id, status, create_time, id),
    INDEX idx_one_lv_like (article_id, level_one_id, status, like_count DESC, id),
    INDEX idx_two_lv_time (`level_one_id`, `parent_id`, `create_time`),
    INDEX idx_levelOne_two_levelTwo (level_one_id, deleted, parent_id, like_count DESC, id),
    INDEX idx_levelOne_deleted (`level_one_id`, `deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '文章评论表';

-- 评论点赞表
CREATE TABLE IF NOT EXISTS TB_WZY_BLOG_COMMENT_LIKE
(
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `user_id`    BIGINT NOT NULL COMMENT '用户ID',
    PRIMARY KEY (`comment_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '评论点赞表';

-- 本地消息表
CREATE TABLE IF NOT EXISTS TB_WZY_BLOG_NOTIFY_OUTBOX
(
    `id`                BIGINT       NOT NULL PRIMARY KEY COMMENT '主键id',
    `version`           INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `deleted`           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除 0-正常 1-删除',
    `create_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `comment_id`        BIGINT       NOT NULL COMMENT '评论ID',
    `user_id`           BIGINT       NOT NULL COMMENT '点赞者ID',
    `recipient_user_id` BIGINT       NOT NULL COMMENT '接收者ID',
    `status`            TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0 NEW 未发送/ 1 SENDING 发送中/ 2 SENT 发送成功/ 3 FAILED 发送失败',
    `owner`             VARCHAR(256) NULL COMMENT '本次派发者实例标识',
    `claim_time`        TIMESTAMP    NULL COMMENT '抢占时间',
    INDEX `idx_claim_pick` (status, id),
    INDEX `idx_owner_status_id` (owner, status, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '本地消息表';