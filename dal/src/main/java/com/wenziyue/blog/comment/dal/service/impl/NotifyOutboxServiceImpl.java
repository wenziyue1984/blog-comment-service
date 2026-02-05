package com.wenziyue.blog.comment.dal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wenziyue.blog.comment.dal.entity.NotifyOutboxEntity;
import com.wenziyue.blog.comment.dal.mapper.NotifyOutboxMapper;
import com.wenziyue.blog.comment.dal.service.NotifyOutboxService;
import org.springframework.stereotype.Service;

/**
 * @author wenziyue
 */
@Service
public class NotifyOutboxServiceImpl extends ServiceImpl<NotifyOutboxMapper, NotifyOutboxEntity> implements NotifyOutboxService {
    @Override
    public int batchSetOwner(String ownerToken, Integer batchSize) {
        return baseMapper.batchSetOwner(ownerToken, batchSize);
    }
}
