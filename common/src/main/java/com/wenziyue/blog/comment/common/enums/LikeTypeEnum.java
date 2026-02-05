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
public enum LikeTypeEnum implements ICommonEnum {

    LIKE(0, "点赞"),
    CANCEL_LIKE(1, "取消点赞");

    @EnumValue
    private final Integer code;
    private final String desc;
}
