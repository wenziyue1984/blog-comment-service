package com.wenziyue.blog.comment.dal.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wenziyue.blog.comment.dal.dto.CommentDTO;
import com.wenziyue.blog.comment.dal.dto.CommentLikeDeltaDTO;
import com.wenziyue.blog.comment.dal.dto.CommentPageDTO;
import com.wenziyue.blog.comment.dal.entity.CommentEntity;
import com.wenziyue.blog.comment.dal.mapper.CommentMapper;
import com.wenziyue.blog.comment.dal.service.CommentService;
import com.wenziyue.mybatisplus.page.PageRequest;
import com.wenziyue.mybatisplus.page.PageResult;
import lombok.val;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wenziyue
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, CommentEntity> implements CommentService {
    @Override
    public PageResult<CommentDTO> oneLevelCommentPage(PageRequest dto, Long articleId, Integer sort) {
        Page<CommentDTO> page = new Page<>(dto.getCurrent(), dto.getSize());
        val result = baseMapper.oneLevelCommentPage(page, articleId, sort);
        return PageResult.<CommentDTO>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .size(result.getSize())
                .current(result.getCurrent())
                .pages(result.getPages())
                .build();
    }

    @Override
    public List<CommentDTO> getTwoLevelCommentForOneLevelComment(List<Long> oneLevelCommentIdList, int limit) {
        return this.baseMapper.getTwoLevelCommentForOneLevelComment(oneLevelCommentIdList, limit);
    }

    @Override
    public PageResult<CommentDTO> twoLevelCommentPage(CommentPageDTO dto, Long articleId, Long oneLevelCommentId) {
        Page<CommentDTO> page = new Page<>(dto.getCurrent(), dto.getSize());
        val result = baseMapper.twoLevelCommentPage(page, articleId, oneLevelCommentId);
        return PageResult.<CommentDTO>builder()
                .records(result.getRecords())
                .total(result.getTotal())
                .size(result.getSize())
                .current(result.getCurrent())
                .pages(result.getPages())
                .build();
    }

    @Override
    public int batchApplyLikeDeltas(List<CommentLikeDeltaDTO> items) {
        return baseMapper.batchApplyLikeDeltas(items);
    }
}
