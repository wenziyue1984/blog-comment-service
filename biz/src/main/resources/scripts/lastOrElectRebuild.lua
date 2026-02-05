-- KEYS[1] = seqKey
-- KEYS[2] = rebuildKey
-- ARGV[1] = ttlSec
-- ARGV[2] = rebuildLockMs

local ttlSec       = tonumber(ARGV[1])
local rebuildLockMs= tonumber(ARGV[2])

local v = redis.call('GET', KEYS[1])
if v then
    -- 刷新 TTL（可选，你也可以不刷新）
    if ttlSec and ttlSec > 0 then
        redis.call('EXPIRE', KEYS[1], ttlSec)
    end
    return {1, tonumber(v)}   -- code=1, value=当前 seq
end

-- 不存在，看看是否已有人在重建
if redis.call('EXISTS', KEYS[2]) == 1 then
    return {3, 0}             -- code=3, 他人重建中
end

-- 我来重建：放置一个短锁
redis.call('PEXSET', KEYS[2], '1', 'PX', rebuildLockMs)
return {2, 0}               -- code=2, 我当选重建者