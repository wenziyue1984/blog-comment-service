package com.wenziyue.blog.comment.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.wenziyue.framework.common.ICommonEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wenziyue
 */
@Getter
@AllArgsConstructor
public enum CommentDepthEnum implements ICommonEnum {

    ONE_LEVEL(0, "一级评论"),
    TWO_LEVEL(1, "二级评论");

    @EnumValue
    private final Integer code;
    private final String desc;
}
