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
public enum CommentOrderByEnum implements ICommonEnum {

    TIME_ASC(0, "时间正序"),
    TIME_DESC(1, "时间倒序"),
    LIKE_COUNT_DESC(2, "点赞数高到低");

    @EnumValue
    private final Integer code;
    private final String desc;
}
