package com.wenziyue.blog.comment.biz.service;

import com.wenziyue.blog.comment.dal.dto.CommentDTO;
import com.wenziyue.blog.comment.dal.dto.CommentPageDTO;
import com.wenziyue.mybatisplus.page.PageResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author wenziyue
 */
public interface BizCommentService {

    @Transactional
    Long postComment(CommentDTO dto, Long userId);

    void likeComment(Long commentId, Long userId);

    void cancelLikeComment(Long commentId);

    @Transactional(readOnly = true)
    PageResult<CommentDTO> pageOneLevelComment(CommentPageDTO dto);

    @Transactional(readOnly = true)
    PageResult<CommentDTO> pageTwoLevelComment(CommentPageDTO dto);
}
