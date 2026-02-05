package com.wenziyue.blog.comment.common.exception;

import com.wenziyue.framework.common.IResultCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author wenziyue
 */
@Getter
@RequiredArgsConstructor
public enum BlogResultCode implements IResultCode {

    REGISTER_NAME_EXIST("60010", "用户名已存在"),
    REGISTER_EMAIL_EXIST("60020", "邮箱已存在"),
    REGISTER_PHONE_EXIST("60030", "电话已存在"),
    REGISTER_NAME_OR_EMAIL_OR_PHONE_EXIST("60040", "用户名或邮箱或电话已存在"),
    REGISTER_FAIL("60050", "注册失败"),
    REGISTER_PARAM_ERROR("60060", "用户名和密码不能为空"),

    USER_NOT_EXIST("60070", "用户不存在"),
    LOGIN_PARAM_ERROR("60080", "用户名或密码错误"),
    LOGIN_PARAM_IS_EMPTY("60090", "登录参数为空"),
    LOGIN_ERROR("60092", "登录失败"),

    CAPTCHA_IS_EMPTY("60100", "验证码参数为空"),
    CAPTCHA_HAS_EXPIRED("60100", "验证码已失效"),
    CAPTCHA_IS_ERROR("60100", "验证码错误"),

    REFRESH_TOKEN_ERROR("60110", "刷新token失败"),

    UPDATE_USER_INFO_ERROR("60120", "更新用户信息失败"),

    OLD_PASSWORD_EQUAL_NEW_PASSWORD("60130", "新密码和旧密码相同"),

    SLUG_GENERATE_ERROR("60140", "slug生成失败"),

    ARTICLE_TITLE_EMPTY("60150", "文章标题为空"),
    ARTICLE_CONTENT_EMPTY("60160", "文章内容为空"),
    ARTICLE_SAVE_OR_UPDATE_ERROR("60170", "文章保存或修改失败"),
    ARTICLE_TITLE_REPEAT("60180", "文章标题重复"),
    ARTICLE_TAT_EMPTY("60190", "文章标签不存在"),
    ARTICLE_NOT_EXIST("60200", "文章不存在"),

    USER_NO_PERMISSION("60210", "无操作权限"),
    HIDDEN_CANNOT_SET_TOP("60220", "隐藏的文章不能置顶"),
    USER_TOP_ARTICLE_LIMIT("60230", "置顶文章不能超过3篇"),

    USER_CANNOT_LIKE_SELF_ARTICLE("60240", "不能点赞自己的文章"),
    USER_LIKE_ARTICLE_EXIST("60250", "用户已经点赞过该文章"),
    USER_LIKE_ERROR("60260", "点赞失败"),
    USER_CANCEL_LIKE_ERROR("60270", "取消点赞失败"),

    PV_ERROR("60280", "pv统计失败"),

    GLOBAL_RATE_LIMITED("60281", "操作太频繁"),

    FAVORITES_FOLDER_NAME_REPEAT("60290", "收藏夹重名"),
    FAVORITES_FOLDER_NOT_EXIST("60300", "收藏夹不存在"),
    FAVORITES_ARTICLE_EXIST("60310", "文章已收藏"),
    FAVORITES_ARTICLE_NOT_EXIST("60320", "收藏夹中并没有收藏该文章"),

    COMMENT_NOT_EXIST("60330", "评论不存在"),
    COMMENT_CONTENT_EMPTY("60340", "评论内容为空"),
    COMMENT_OVER_TEN_TIMES("60350", "评论过于频繁"),
    COMMENT_CONTENT_REPEAT("60360", "频繁发布相同内容"),
    COMMENT_LIKE_ERROR("60370", "点赞失败"),
    COMMENT_CANCEL_LIKE_ERROR("60380", "取消点赞失败"),

    CHAT_RECORD_NOT_EXIST("60390", "聊天记录不存在"),
    CHAT_SESSION_LAST_SEQ_ERROR("60400", "获取会话last_seq失败"),
    CHAT_SESSION_EMPTY("60410", "会话不存在"),

    NOTIFY_NOT_EXIST("60420", "通知不存在"),
    ;

    private final String code;
    private final String msg;

}
