package com.wenziyue.blog.comment.infra.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.wenziyue.framework.common.ICommonEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举
 * 1 - 正常，0 - 禁用
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum implements ICommonEnum {

    DISABLED(1, "禁用"),
    ENABLED(0, "正常");

    @EnumValue
    private final Integer code;
    private final String desc;
}
