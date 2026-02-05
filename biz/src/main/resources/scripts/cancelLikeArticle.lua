-- KEYS[1] = 用户点赞文章 ZSet（user_like_articles_key）
-- KEYS[2] = 文章总点赞数 key（article_like_count_key）
-- KEYS[3] = 文章点赞用户 ZSet（article_like_users_key）
-- KEYS[4] = 行为记录 Stream key（article_like_stream_key）

-- ARGV[1] = 用户ID
-- ARGV[2] = 文章ID
-- ARGV[3] = 当前时间戳
-- ARGV[4] = 行为类型（CANCEL_LIKE = 2）


-- 没有点赞直接退出
if not redis.call('ZSCORE', KEYS[1], ARGV[2]) then
    return true
end

-- 如果用户点赞记录中存在该文章，移除
redis.call('ZREM', KEYS[1], ARGV[2])

-- 如果文章点赞用户列表中有该用户，也移除
redis.call('ZREM', KEYS[3], ARGV[1])

-- 点赞数 -1（最小为0）
local current = redis.call('GET', KEYS[2])
if current and tonumber(current) > 0 then
    redis.call('DECR', KEYS[2])
end

-- 写入行为日志（使用自动生成的 RecordId，并且最大长度100000，防止撑爆redis）
redis.call('XADD', KEYS[4], 'MAXLEN', '~', 100000, '*',
        'userId', ARGV[1],
        'articleId', ARGV[2],
        'time', ARGV[3],
        'type', ARGV[4]
)

return true