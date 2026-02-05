package com.wenziyue.blog.comment.dal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wenziyue.blog.comment.dal.dto.CommentLikeMqDTO;
import com.wenziyue.blog.comment.dal.entity.CommentLikeEntity;

import java.util.List;

/**
 * @author wenziyue
 */
public interface CommentLikeService extends IService<CommentLikeEntity> {


    void insertIgnoreBatch(List<CommentLikeMqDTO> likes);

    void deleteBatch(List<CommentLikeMqDTO> cancels);

    List<CommentLikeEntity> selectExistingPairs(List<CommentLikeMqDTO> likeList);
}
