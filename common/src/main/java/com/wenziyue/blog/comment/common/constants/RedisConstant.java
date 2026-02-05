package com.wenziyue.blog.comment.common.constants;

/**
 * @author wenziyue
 */
public interface RedisConstant {

    String ID_GENERATOR_KEY = "blog:id";

    /**
     * 登录时存储用户信息
     */
    String LOGIN_TOKEN_KEY = "blog:login:userDetail:";

    /**
     * 用户所有活跃的token集合
     */
    String USER_TOKENS_KEY = "blog:login:userTokens:";

    /**
     * 验证码
     */
    String CAPTCHA_KEY = "blog:captcha:";

    /**
     * 监听生成的slug
     */
    String SLUG_LISTEN_KEY = "blog:slug:listen:";

    /**
     * 文章更新时间，防止生成summary和slug的时候文章已被修改，也可验证当前文章有无加入mq队列
     */
    String ARTICLE_UPDATE_TIME_KEY = "blog:article:updateTime:";

    /**
     * 文章点赞数量
     */
    String ARTICLE_LIKE_COUNT_KEY = "blog:article:like:count:";

    /**
     * 文章点赞用户列表
     * article:like:users:{articleId} → ZSet<userId, timestamp>
     */
    String ARTICLE_LIKE_USERS_KEY = "blog:article:like:users:";

    /**
     * 用户点赞的文章列表
     * user:like:articles:{userId} → ZSet<articleId, timestamp>
     */
    String USER_LIKE_ARTICLES_KEY = "user:like:articles:";

    /**
     * 文章点赞流
     */
    String ARTICLE_LIKE_STREAM_KEY = "blog:article:like:stream";

    /**
     * 文章点赞流组
     */
    String ARTICLE_LIKE_STREAM_GROUP_NAME = "like_group";

    /**
     * 文章点赞流消费者
     */
    String ARTICLE_LIKE_STREAM_CONSUMER_NAME = "consumer_1";

    /**
     * 文章pv
     */
    String ARTICLE_PV_COUNT_KEY = "blog:article:pv:count:";

    /**
     * 文章pv过滤
     * blog:article:pv:filter:{articleId}+{sessionId}+{ip}
     */
    String ARTICLE_PV_FILTER_KEY = "blog:article:pv:filter:";

    /**
     * 文章pv布隆过滤器
     * blog:article:pv:bloom:{articleId}+{index}
     */
    String ARTICLE_PV_BLOOM_KEY = "blog:article:pv:bloom:";

    /**
     * 每篇文章有几个bloom过滤器
     * blog:article:pv:bloom:num:set:{articleId}
     */
    String ARTICLE_PV_BLOOM_NUM_SET_KEY = "blog:article:pv:bloom:num:set:";

    /**
     * 评论检查key
     * blog:comment:check:{userId}
     */
    String COMMENT_CHECK_KEY = "blog:comment:check:";

    /**
     * 限流key前缀
     * blog:limitRate:{业务自己传入}
     */
    String LIMIT_RATE_PREFIX = "blog:limitRate:";

    /**
     * 评论点赞布谷过滤器，用于检测用户是否点赞过该评论
     * blog:comment:like:cuckoo:{date}
     */
    String COMMENT_LIKE_CUCKOO_PREFIX = "blog:comment:like:cuckoo:";

    /**
     * 评论点赞滑窗过滤器，用于检测短期内用户是否点赞过该评论
     * blog:comment:like:sluice:gate:{commentId+userId}
     */
    String COMMENT_LIKE_SLUICE_GATE = "blog:comment:like:sluice:gate:";

    /**
     * 保存点赞数的hash前缀
     * blog:comment:like:count:{sh + index}
     */
    String COMMENT_LIKE_COUNT_HASH_PREFIX = "blog:comment:like:count:";

    /**
     * 同步点赞数锁
     * blog:comment:like:sync:lock
     */
    String COMMENT_LIKE_SYNC_LOCK_KEY = "blog:comment:like:sync:lock";


    /**
     * 聊天会话
     * blog:chat:session:key:{smallUserId}:{bigUserId}
     */
    String CHAT_SESSION_KEY = "blog:chat:session:key:";

    /**
     * 聊天会话序号
     * blog:chat:session:seq:{smallUserId}:{bigUserId}
     */
    String CHAT_SESSION_SEQ_KEY = "blog:chat:session:seq:";

    /**
     * 聊天会话序号锁
     * blog:chat:session:seq:lock:{sessionId}
     */
    String CHAT_SESSION_SEQ_LOCK_KEY = "blog:chat:session:seq:lock:";

    /**
     * 聊天会话序号重建标识
     * blog:chat:session:seq:rebuild:{smallUserId}:{bigUserId}
     */
    String CHAT_SESSION_SEQ_REBUILD_KEY = "blog:chat:session:seq:rebuild:";

    /**
     * 每个会话的聊天消息id集合
     * blog:chat:msgId:set:{smallUserId}:{bigUserId}:{date}
     */
    String CHAT_MSG_ID_SET_KEY = "blog:chat:msgId:set:";

    /**
     * 每个会话的聊天消息最后时间的有序集合，
     * key是sessionId，score是时间戳，最大容量10000
     * blog:chat:session:msg:lastTime:{userId}
     */
    String CHAT_SESSION_MSG_LAST_TIME_KEY = "blog:chat:session:msg:last:time:";

    /**
     * 通知消息发送状态
     * blog:notify:outbox:has:sent:{notifyId}
     */
    String NOTIFY_OUTBOX_HAS_SENT_KEY = "blog:notify:outbox:has:sent:";

}
