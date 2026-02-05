package com.wenziyue.blog.comment.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenziyue.blog.comment.dal.dto.CommentDTO;
import com.wenziyue.blog.comment.dal.dto.CommentLikeDeltaDTO;
import com.wenziyue.blog.comment.dal.entity.CommentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author wenziyue
 */
public interface CommentMapper extends BaseMapper<CommentEntity> {

    List<CommentDTO> getTwoLevelCommentForOneLevelComment(@Param("oneLevelCommentIdList")List<Long> oneLevelCommentIdList, @Param("limit") int limit);

    IPage<CommentDTO> oneLevelCommentPage(Page<?> page, @Param("articleId") Long articleId, @Param("sort") Integer sort);

    IPage<CommentDTO> twoLevelCommentPage(Page<?> page, @Param("articleId") Long articleId, @Param("oneLevelCommentId") Long oneLevelCommentId);

    int batchApplyLikeDeltas(@Param("items") List<CommentLikeDeltaDTO>  items);
}
