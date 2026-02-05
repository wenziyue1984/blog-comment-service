package com.wenziyue.blog.comment.common.utils;

import com.google.common.hash.Hashing;
import lombok.val;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import static com.wenziyue.blog.comment.common.constants.RedisConstant.*;

/**
 * 博客工具类
 *
 * @author wenziyue
 */
public class BlogUtils {

    /**
     * 安全的trim
     */
    public static String safeTrim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * safeTrim增强，如果safeTrim()返回空字符串，则返回null
     * 防止数据库插入空字符串
     */
    public static String safeTrimEmptyIsNull(String str) {
        val trim = safeTrim(str);
        return trim == null || trim.isEmpty() ? null : trim;
    }

    /**
     * 使用 Murmur3_128 计算指纹，取低 64 位作为最终输出，避免 32 位十六进制过长。
     */
    public static String fp(String... parts) {
        String raw = String.join("|", parts);
        long hash64 = Hashing.murmur3_128()
                .hashString(raw, StandardCharsets.UTF_8)
                .asLong();
        return Long.toUnsignedString(hash64);
    }

    /**
     * 使用 Murmur3_128 计算指纹，返回 32 字节十六进制
     * 但一般不需要这么长。
     */
    public static String fpHex128(String... parts) {
        String raw = String.join("|", parts);
        return Hashing.murmur3_128()
                .hashString(raw, StandardCharsets.UTF_8)
                .toString(); // 32个hex字符
    }

    /**
     * 获取当天日期的YYYYMMDD
     */
    public static String getTodayDateFormat() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 获取昨天日期的YYYYMMDD
     */
    public static String getYesterdayDateFormat() {
        return LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 获取明天日期的YYYYMMDD
     */
    public static String getTomorrowDateFormat() {
        return LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 将评论id进行分片
     * @param commentId 评论id
     * @return 分片值，范围 0~63
     */
    public static int shardOfMurmur(String commentId) {
        int h = Hashing.murmur3_32_fixed().hashUnencodedChars(commentId).asInt();
        return h & 63; // 低 6 位，等价于 (h & 0x7fffffff) % 64
    }

    /**
     * 获取评论点赞的分片key,带 hash tag，保证 srcKey 与 drainKey 在redis集群下同槽位
     * @param shard 分片值
     */
    public static String shardKey(int shard) {
        return COMMENT_LIKE_COUNT_HASH_PREFIX + "{sh" + shard + "}";
    }

    /**
     * 根据评论id获取评论点赞hash的分片key
     * @param commentId 评论id
     */
    public static String getCommentLikeCountHashKey(String commentId) {
        return shardKey(shardOfMurmur(commentId));
    }

    /**
     * 获取今天评论点赞cf的key
     */
    public static String getTodayCfKey() {
        return COMMENT_LIKE_CUCKOO_PREFIX + getTodayDateFormat();
    }

    /**
     * 获取明天评论点赞cf的key
     */
    public static String getTomorrowCfKey() {
        return COMMENT_LIKE_CUCKOO_PREFIX + getTomorrowDateFormat();
    }

    /**
     * 获取昨天评论点赞cf的key
     */
    public static String getYesterdayCfKey() {
        return COMMENT_LIKE_CUCKOO_PREFIX + getYesterdayDateFormat();
    }

    /**
     * 点赞评论闸门key
     * @param commentId 评论id
     * @param userId 用户id
     */
    public static String getSluiceGateKey (String commentId, String userId) {
        return COMMENT_LIKE_SLUICE_GATE + commentId + ":" + userId;
    }

    /**
     * 获取评论点赞hash的drain key
     * @param shard 分片值
     */
    public static String getCommentLikeHashDrainKey(int shard) {
        return shardKey(shard) + ":drain:" + System.currentTimeMillis();
    }

    /**
     * 获取评论点赞hash的pattern key
     * @param shard 分片值
     */
    public static String getCommentLikeHashPatternKey(int shard) {
        return shardKey(shard) + ":drain:*";
    }

    /**
     * 生成redis缓存key：ChatSessionKey
     */
    public static String getChatSessionKey(Long id1, Long id2) {
        val idArray = compareIdSize(id1, id2);
        return CHAT_SESSION_KEY + idArray[0] + ":" + idArray[1];
    }

    /**
     * 比较两个id的大小
     */
    public static Long[] compareIdSize(Long id1, Long id2) {
        return id1.compareTo(id2) <= 0 ? new Long[]{id1, id2} : new Long[]{id2, id1};
    }

    /**
     * 休眠
     */
    public static void sleepJitter(long baseMs) {
        try {
            Thread.sleep(baseMs + ThreadLocalRandom.current().nextInt(30));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取今天聊天msgId set的key
     */
    public static String getTodayChatMsgIdSetKey(Long smallUserId, Long bigUserId) {
        return CHAT_MSG_ID_SET_KEY + smallUserId + ":" + bigUserId + ":" + getTodayDateFormat();
    }

    /**
     * 获取昨天评论点赞cf的key
     */
    public static String getYesterdayChatMsgIdSetKey(Long smallUserId, Long bigUserId) {
        return CHAT_MSG_ID_SET_KEY + smallUserId + ":" + bigUserId + ":" + getYesterdayDateFormat();
    }

    /**
     * 获取聊天会话seq的key
     */
    public static String getChatSessionSeqKey(Long smallUserId, Long bigUserId) {
        return CHAT_SESSION_SEQ_KEY + smallUserId + ":" + bigUserId;
    }

    /**
     * 获取聊天会话seq重建的key
     */
    public static String getChatSessionSeqRebuildKey(Long smallUserId, Long bigUserId) {
        return CHAT_SESSION_SEQ_REBUILD_KEY + smallUserId + ":" + bigUserId;
    }

}
