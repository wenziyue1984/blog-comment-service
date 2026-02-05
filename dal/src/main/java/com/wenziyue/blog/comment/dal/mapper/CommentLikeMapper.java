package com.wenziyue.blog.comment.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenziyue.blog.comment.dal.dto.CommentLikeMqDTO;
import com.wenziyue.blog.comment.dal.entity.CommentLikeEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author wenziyue
 */
public interface CommentLikeMapper extends BaseMapper<CommentLikeEntity> {

    int insertIgnoreBatch(@Param("items") List<CommentLikeEntity> items);
    int deleteBatch(@Param("items") List<CommentLikeEntity> items);

    List<CommentLikeEntity> selectExistingPairs(@Param("items") List<CommentLikeMqDTO> likeList);
}
