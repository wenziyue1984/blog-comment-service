package com.wenziyue.blog.comment.common.constants;

/**
 * @author wenziyue
 */
public interface RocketMqTopic {

    String SlugTopic = "blog_slug";

    String SummaryTopic = "blog_summary";

    String ArticlePvTopic = "blog_article_pv";

    String CommentLikeTopic = "blog_comment_like";

    String ChatRecordsSaveTopic = "blog_chat_records_save";

    String NotifyOutboxTopic = "blog_notify_outbox";
}
