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
public enum NotifyOutboxStatusEnum implements ICommonEnum {

    //0 NEW 未发送/ 1 SENDING 发送中/ 2 SENT 发送成功/ 3 FAILED 发送失败
    NEW(0, "未发送"),
    SENDING(1, "发送中"),
    SENT(2, "发送成功"),
    FAILED(3, "发送失败");


    @EnumValue
    private final Integer code;
    private final String desc;
}
