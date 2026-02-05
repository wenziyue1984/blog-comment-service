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
public enum CommentStatusEnum implements ICommonEnum {

    NORMAL(0, "正常"),
    HIDDEN(1, "隐藏");

    @EnumValue
    private final Integer code;
    private final String desc;
}
