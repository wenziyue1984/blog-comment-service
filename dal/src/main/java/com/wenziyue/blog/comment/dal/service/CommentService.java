package com.wenziyue.blog.comment.dal.service;

import com.wenziyue.blog.comment.dal.dto.CommentDTO;
import com.wenziyue.blog.comment.dal.dto.CommentLikeDeltaDTO;
import com.wenziyue.blog.comment.dal.dto.CommentPageDTO;
import com.wenziyue.blog.comment.dal.entity.CommentEntity;
import com.wenziyue.mybatisplus.base.PageExtendService;
import com.wenziyue.mybatisplus.page.PageRequest;
import com.wenziyue.mybatisplus.page.PageResult;

import java.util.List;

/**
 * @author wenziyue
 */
public interface CommentService extends PageExtendService<CommentEntity> {

    PageResult<CommentDTO> oneLevelCommentPage(PageRequest dto, Long articleId, Integer sort);
    List<CommentDTO> getTwoLevelCommentForOneLevelComment(List<Long> oneLevelCommentIdList, int limit);
    PageResult<CommentDTO> twoLevelCommentPage(CommentPageDTO dto, Long articleId, Long oneLevelCommentId);

    int batchApplyLikeDeltas(List<CommentLikeDeltaDTO> items);
}
