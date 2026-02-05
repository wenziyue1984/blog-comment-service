package com.wenziyue.blog.comment.dal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wenziyue.blog.comment.dal.entity.NotifyOutboxEntity;

/**
 * @author wenziyue
 */
public interface NotifyOutboxService extends IService<NotifyOutboxEntity> {

    /**
     * 批量设置owner,并且直接将status设置为1发送中
     * @param ownerToken 处理线程标识
     * @param batchSize 批量处理数量
     * @return update成功数量
     */
    int batchSetOwner(String ownerToken, Integer batchSize);
}
