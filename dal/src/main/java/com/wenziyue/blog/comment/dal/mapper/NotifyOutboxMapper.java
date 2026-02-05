package com.wenziyue.blog.comment.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenziyue.blog.comment.dal.entity.NotifyOutboxEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @author wenziyue
 */
public interface NotifyOutboxMapper extends BaseMapper<NotifyOutboxEntity> {

    @Update("UPDATE TB_WZY_BLOG_NOTIFY_OUTBOX " +
            "SET status = 1, owner = #{ownerToken}, claim_time = NOW() " +
            "WHERE status = 0 " +
            "ORDER BY id " +
            "LIMIT #{batchSize}")
    int batchSetOwner(@Param("ownerToken") String ownerToken, @Param("batchSize") int batchSize);
}
