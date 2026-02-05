-- KEYS[1] = seqKey
-- KEYS[2] = rebuildKey
-- ARGV[1] = base
-- ARGV[2] = ttlSec

local base  = tonumber(ARGV[1])
local ttlSec= tonumber(ARGV[2])

local cur = redis.call('GET', KEYS[1])
if cur then
    -- 已经有人设过了，直接返回更大的那个
    return tonumber(cur)
end

-- 没有就写入 base + TTL
redis.call('SET', KEYS[1], tostring(base))
if ttlSec and ttlSec > 0 then
    redis.call('EXPIRE', KEYS[1], ttlSec)
end

-- 可选：立刻删除重建标志；也可以让它自然过期
redis.call('DEL', KEYS[2])

return base