package com.wenziyue.blog.comment.web.controller;

import com.wenziyue.auth.starter.helper.AuthHelper;
import com.wenziyue.blog.comment.biz.service.BizCommentService;
import com.wenziyue.blog.comment.dal.dto.CommentDTO;
import com.wenziyue.blog.comment.dal.dto.CommentPageDTO;
import com.wenziyue.blog.comment.infra.client.UserClient;
import com.wenziyue.blog.comment.infra.dto.UserInfoFeignDTO;
import com.wenziyue.framework.annotation.ResponseResult;
import com.wenziyue.framework.exception.ApiException;
import com.wenziyue.mybatisplus.page.PageResult;
import feign.FeignException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * @author wenziyue
 */
@RestController
@RequestMapping("/comment")
@Slf4j
@RequiredArgsConstructor
@ResponseResult
@Tag(name = "评论管理", description = "评论相关接口")
public class CommentController {

    private final BizCommentService bizCommentService;
    private final UserClient userClient;


    @Operation(summary = "发布评论", description = "发布评论")
    @PostMapping("/postComment")
    @PreAuthorize("hasAuthority('USER')")
    public Long postComment(@Parameter(description = "发布评论参数", required = true) @Valid @RequestBody CommentDTO dto) {
        return bizCommentService.postComment(dto, Long.valueOf(AuthHelper.currentUserId()));
    }

    @Operation(summary = "点赞评论", description = "点赞评论")
    @GetMapping("/likeComment/{commentId}")
    @PreAuthorize("hasAuthority('USER')")
    public void likeComment(@Parameter(description = "点赞评论参数", required = true) @PathVariable Long commentId) {
        bizCommentService.likeComment(commentId, Long.valueOf(AuthHelper.currentUserId()));
    }

    @Operation(summary = "取消点赞评论", description = "取消点赞评论")
    @GetMapping("/cancelLikeComment/{commentId}")
    @PreAuthorize("hasAuthority('USER')")
    public void cancelLikeComment(@Parameter(description = "点赞评论参数", required = true) @PathVariable Long commentId) {
        bizCommentService.cancelLikeComment(commentId);
    }

    @Operation(summary = "一级评论分页列表", description = "一级评论分页列表，带两条二级评论")
    @PostMapping("/pageOneLevelComment")
    public PageResult<CommentDTO> pageOneLevelComment(@Parameter(description = "分页参数", required = true) @Valid @RequestBody CommentPageDTO dto) {
        return bizCommentService.pageOneLevelComment(dto);
    }

    @Operation(summary = "二级评论分页列表", description = "二级评论分页列表")
    @PostMapping("/pageTwoLevelComment")
    public PageResult<CommentDTO> pageTwoLevelComment(@Parameter(description = "分页参数", required = true) @Valid @RequestBody CommentPageDTO dto) {
        return bizCommentService.pageTwoLevelComment(dto);
    }

    @Operation(summary = "测试feign", description = "测试feign")
    @GetMapping("/testFeign/{id}")
    public UserInfoFeignDTO testFeign(@PathVariable Long id) {
        log.info("测试feign");
        try {
            UserInfoFeignDTO userInfoDTO = userClient.userInfo(id);
            return userInfoDTO;
        } catch (ApiException e) {
            log.error("feign调用失败ApiException", e);
            throw new RuntimeException(e);
        } catch (FeignException e) {
            log.error("feign调用失败FeignException", e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            log.error("feign调用失败", e);
            throw new RuntimeException(e);
        }
    }


}
