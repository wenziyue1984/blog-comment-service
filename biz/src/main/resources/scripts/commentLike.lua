-- KEYS[1] = CF key（例如 "comment:like:cf:2025-08-15"）
-- KEYS[2] = Hash key（某一分片，例如 "blog:comment:like:count:{sh42}"）
-- KEYS[3] = 闸门 key（例如 "comment:like:gate:commentId:userId"）
--
-- ARGV[1] = CF item（"commentId:userId"）
-- ARGV[2] = Hash field（commentId）
-- ARGV[3] = delta（点赞传 "1"）
-- ARGV[4] = 闸门写入的值（例如 "1"）
-- ARGV[5] = 闸门 TTL（秒；传 "0" 或空串则不设置过期）
--
-- 返回：{ added, newCnt }
--   added = CF.ADDNX 的结果：1 表示本次新增，0 表示可能已存在
--   newCnt = HINCRBY 之后 hash 字段的新值

local added = redis.call('CF.ADDNX', KEYS[1], ARGV[1])
local newCnt = redis.call('HINCRBY', KEYS[2], ARGV[2], tonumber(ARGV[3]))

if ARGV[5] ~= '' and tonumber(ARGV[5]) > 0 then
    redis.call('SET', KEYS[3], ARGV[4], 'EX', tonumber(ARGV[5]))
else
    redis.call('SET', KEYS[3], ARGV[4])
end

return { added, newCnt }