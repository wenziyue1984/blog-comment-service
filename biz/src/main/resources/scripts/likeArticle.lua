-- KEYS[1] = 用户点赞文章 ZSet（user_like_articles_key）
-- KEYS[2] = 文章总点赞数 key（article_like_count_key）
-- KEYS[3] = 文章点赞用户 ZSet（article_like_users_key）
-- KEYS[4] = 行为记录 Stream key（article_like_stream_key）

-- ARGV[1] = 用户ID
-- ARGV[2] = 文章ID
-- ARGV[3] = 当前时间戳
-- ARGV[4] = 最大点赞用户数
-- ARGV[5] = 点赞类型（如 LIKE = 1）

-- 已点赞直接退出
if redis.call('ZSCORE', KEYS[1], ARGV[2]) then
    return true
end

-- 添加到用户点赞文章 ZSet
redis.call('ZADD', KEYS[1], ARGV[3], ARGV[2])

-- 文章点赞数加一
redis.call('INCR', KEYS[2])

-- 如果文章点赞用户数小于限制，添加到文章点赞用户 ZSet
local currentSize = redis.call('ZCARD', KEYS[3])
if tonumber(currentSize) < tonumber(ARGV[4]) then
    redis.call('ZADD', KEYS[3], ARGV[3], ARGV[1])
end

-- 写入行为日志（使用自动生成的 RecordId，并且最大长度100000，防止撑爆redis）
redis.call('XADD', KEYS[4], 'MAXLEN', '~', 100000, '*',
        'userId', ARGV[1],
        'articleId', ARGV[2],
        'time', ARGV[3],
        'type', ARGV[5]
)

return true