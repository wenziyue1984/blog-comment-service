package com.wenziyue.blog.comment.biz.service.impl;

import com.wenziyue.blog.comment.infra.client.ArticleClient;
import com.wenziyue.auth.starter.helper.AuthHelper;
import com.wenziyue.blog.comment.biz.service.BizCommentService;
import com.wenziyue.blog.comment.biz.utils.IdUtils;
import com.wenziyue.blog.comment.common.annotation.WzyRateLimiter;
import com.wenziyue.blog.comment.common.enums.CommentDepthEnum;
import com.wenziyue.blog.comment.common.enums.LikeTypeEnum;
import com.wenziyue.blog.comment.common.utils.BlogUtils;
import com.wenziyue.blog.comment.dal.dto.CommentDTO;
import com.wenziyue.blog.comment.dal.dto.CommentLikeMqDTO;
import com.wenziyue.blog.comment.dal.dto.CommentPageDTO;
import com.wenziyue.blog.comment.dal.entity.CommentEntity;
import com.wenziyue.blog.comment.dal.service.CommentService;
import com.wenziyue.blog.comment.infra.client.UserClient;
import com.wenziyue.blog.comment.infra.dto.UserInfoFeignDTO;
import com.wenziyue.framework.exception.ApiException;
import com.wenziyue.mybatisplus.page.PageResult;
import com.wenziyue.redis.utils.RedisUtils;
import com.wenziyue.uid.core.IdGen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.wenziyue.blog.comment.common.constants.RedisConstant.COMMENT_CHECK_KEY;
import static com.wenziyue.blog.comment.common.constants.RocketMqTopic.CommentLikeTopic;
import static com.wenziyue.blog.comment.common.exception.BlogResultCode.*;

/**
 * @author wenziyue
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizCommentServiceImpl implements BizCommentService {

    private final CommentService commentService;
    private final RedisUtils redisUtils;
    private final IdGen idGen;
    private final RocketMQTemplate rocketMQTemplate;
    private final ArticleClient articleClient;
    private final UserClient userClient;

    @Override
    @WzyRateLimiter(key = "'comment:postComment:' + #userId", window = 60000, maxCount = 10, message = "评论太频繁")
    public Long postComment(CommentDTO dto, Long userId) {
        val content = BlogUtils.safeTrimEmptyIsNull(dto.getContent());
        if (content == null) {
            throw new ApiException(COMMENT_CONTENT_EMPTY);
        }
        // 防止刷评论
        check(content, dto.getArticleId(), userId);
        val articleDTO = articleClient.getArticleById(dto.getArticleId());
        if (articleDTO == null) {
            throw new ApiException(ARTICLE_NOT_EXIST);
        }
        Long replyUserId = articleDTO.getUserId();
        if (dto.getParentId() != null) {
            val parentCommentEntity = commentService.getById(dto.getParentId());
            if (parentCommentEntity == null) {
                throw new ApiException(COMMENT_NOT_EXIST);
            }
            replyUserId = parentCommentEntity.getUserId();
        }
        val id = IdUtils.getID(idGen);
        commentService.save(CommentEntity.builder()
                .id(id)
                .content(content)
                .articleId(dto.getArticleId())
                .authorId(articleDTO.getUserId())
                .parentId(dto.getParentId())
                .userId(userId)
                .replyUserId(replyUserId)
                .depth(dto.getParentId()==null ? CommentDepthEnum.ONE_LEVEL : CommentDepthEnum.TWO_LEVEL)
                .build());
        redisUtils.zAdd(COMMENT_CHECK_KEY + userId, BlogUtils.fp(dto.getArticleId() + ":" + content), System.currentTimeMillis(), 1L, TimeUnit.MINUTES);
        // todo 发送通知消息

        return id;
    }

    /**
     * 判断有没有刷评论，
     * 1.每个用户每分钟只能发十条评论（现在把限流放在了统一注解里，这里不再需要检查）
     * 2.并且每篇文章下同样内容的评论每分钟只能发一次
     * 思路：
     * 用zset，过期时间1min，zset的score就是评论时间，这样每次评论的时候，先去zset中查询，
     * 首先先清理一下zset中score超过1min的评论，然后看剩余的评论数量是否超过10条，如果超过则返回错误，
     * 然后再判断评论有没有重复，如果有则返回错误，如果没有则插入zset中，并且重置zset过期时间，返回成功
     */
    @SuppressWarnings("ConstantConditions")
    private void check(String content, Long articleId, Long userId) {
        String checkKey = COMMENT_CHECK_KEY + userId;
        if (!redisUtils.hasKey(checkKey)) {
            return;
        }
        // 清理过期评论
        redisUtils.zRemoveRangeByScore(checkKey, 0, System.currentTimeMillis() - 60000);
        val objects = redisUtils.zRange(checkKey, 0, -1);
        // 现在把限流放在了统一注解里，这里不再需要检查
//        // 检查一分钟内的评论次数是否超过10
//        if (objects.size() >= 10) {
//            throw new ApiException(COMMENT_OVER_TEN_TIMES);
//        }
        // 检查评论是否重复
        val fp = BlogUtils.fp(articleId + ":" + content);
        val commentFpSet = objects.stream().map(Object::toString).collect(Collectors.toSet());
        if (commentFpSet.contains(fp)) {
            throw new ApiException(COMMENT_CONTENT_REPEAT);
        }
    }

    @Override
    @WzyRateLimiter(key = "'comment:likeComment:' + #userId", window = 60000, maxCount = 30, message = "点赞太频繁")
    public void likeComment(Long commentId, Long userId) {
        likeOrCancelLike(commentId, userId, LikeTypeEnum.LIKE.getCode());
    }

    /**
     * 取消点赞
     * 取消点赞和点赞一样都要走mq，不然用户点赞后立马取消，此时点赞还在mq中排队，没有修改redis计数，也没有落库，你直接取消点赞会发现并没有点赞过，
     * 所以必须让点赞和取消点赞都走mq，并且用rocketMQTemplate.syncSendOrderly()对同一评论进行顺序消费
     */
    @Override
    public void cancelLikeComment(Long commentId) {
        likeOrCancelLike(commentId, Long.valueOf(AuthHelper.currentUserId()), LikeTypeEnum.CANCEL_LIKE.getCode());
    }

    private void likeOrCancelLike(Long commentId, Long userId, int type) {
        val dto = CommentLikeMqDTO.builder().commentId(commentId).userId(userId).type(type).build();
        // 将点赞行为发到mq，使用syncSendOrderly用以将同一个用户同一个评论的点赞行为发送到同一个队列中，以防对同一评论多次点赞的并发问题
        val sendResult = rocketMQTemplate.syncSendOrderly(CommentLikeTopic, dto, commentId + ":" + userId);
        if (sendResult == null || !sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
            log.error("likeOrCancelLike加入mq失败:{}", sendResult);
            throw new ApiException(type == 0 ? COMMENT_LIKE_ERROR : COMMENT_CANCEL_LIKE_ERROR);
        }
    }

    /**
     * 获取一级评论分页，带两条二级评论（如果有二级评论的话）
     */
    @Override
    public PageResult<CommentDTO> pageOneLevelComment(CommentPageDTO dto) {
        val articleDTO = articleClient.getArticleById(dto.getArticleId());
        if (articleDTO == null) {
            throw new ApiException(ARTICLE_NOT_EXIST);
        }
        // 先获取一级评论
        val oneLevelCommentPage = commentService.oneLevelCommentPage(dto, dto.getArticleId(), dto.getSort());
        if (oneLevelCommentPage.getRecords().isEmpty()) {
            return oneLevelCommentPage;
        }

        // 根据一级评论获取两条二级评论
        List<CommentDTO> twoLevelCommentList = commentService.getTwoLevelCommentForOneLevelComment(
                oneLevelCommentPage.getRecords().stream().map(CommentDTO::getId).collect(Collectors.toList()), 2);
        if (twoLevelCommentList.isEmpty()) {
            // 从用户服务获取用户信息
            val userInfoList = userClient.getUserInfoList(oneLevelCommentPage.getRecords().stream().map(CommentDTO::getUserId).collect(Collectors.toSet()));
            if (userInfoList == null || userInfoList.isEmpty()) {
                return oneLevelCommentPage;
            }
            // 组装用户信息
            assembleUserInfo(oneLevelCommentPage.getRecords(), userInfoList);
            return oneLevelCommentPage;
        }
        Set<Long> userIds = oneLevelCommentPage.getRecords().stream().map(CommentDTO::getUserId).collect(Collectors.toSet());
        userIds.addAll(twoLevelCommentList.stream().map(CommentDTO::getUserId).collect(Collectors.toSet()));
        val userInfoList = userClient.getUserInfoList(userIds);
        if (userInfoList != null && !userInfoList.isEmpty()) {
            // 组装用户信息
            assembleUserInfo(oneLevelCommentPage.getRecords(), userInfoList);
            assembleUserInfo(twoLevelCommentList, userInfoList);
        }

        Map<Long, List<CommentDTO>> groupedTwoLevel = twoLevelCommentList.stream()
                .collect(Collectors.groupingBy(CommentDTO::getParentId));

        // 组装
        oneLevelCommentPage.getRecords().forEach(it -> {
            val twoList = groupedTwoLevel.get(it.getId());
            if (twoList != null && !twoList.isEmpty()) {
                it.setChildren(twoList);
                it.setHasMoreChildren(it.getChildrenTotalCount() > it.getChildren().size());
            }
        });
        return oneLevelCommentPage;
    }

    @Override
    public PageResult<CommentDTO> pageTwoLevelComment(CommentPageDTO dto) {
        PageResult<CommentDTO> page = commentService.twoLevelCommentPage(dto, dto.getArticleId(), dto.getOneLevelCommentId());
        if (page.getRecords().isEmpty()) {
            return page;
        }
        // 组装用户信息
        val userInfoList = userClient.getUserInfoList(page.getRecords().stream().map(CommentDTO::getUserId).collect(Collectors.toSet()));
        if (userInfoList != null && !userInfoList.isEmpty()) {
            assembleUserInfo(page.getRecords(), userInfoList);
        }
        return page;
    }

    private void assembleUserInfo(List<CommentDTO> commentDTOList, List<UserInfoFeignDTO> userInfoList) {
        if (commentDTOList == null || commentDTOList.isEmpty() || userInfoList == null || userInfoList.isEmpty()) {
            return;
        }
        commentDTOList.forEach(it -> {
            val userInfo = userInfoList.stream().filter(userInfoDTO -> userInfoDTO.getId().equals(it.getUserId())).findFirst().orElse(null);
            if (userInfo == null) {
                return;
            }
            it.setUserName(userInfo.getName());
            it.setUserAvatarUrl(userInfo.getAvatarUrl());
        });
    }

}

