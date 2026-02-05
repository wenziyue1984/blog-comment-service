-- KEYS[1] = seqKey
-- KEYS[2] = rebuildKey
-- ARGV[1] = ttlSeconds (string)
-- ARGV[2] = rebuildLockMillis (string)

local exists = redis.call('EXISTS', KEYS[1])
if exists == 1 then
    local next = redis.call('INCR', KEYS[1])
    local ttl = tonumber(ARGV[1] or '0')
    if ttl and ttl > 0 then
        redis.call('EXPIRE', KEYS[1], ttl)
    end
    return {1, next}
end

-- seqKey 不存在，看是否有人正在重建
local got = redis.call('SETNX', KEYS[2], '1')
if got == 1 then
    -- 我当选重建者，给 rebuildKey 设置一个短 TTL，防止挂死
    local ms = tonumber(ARGV[2] or '5000')
    if ms and ms > 0 then
        redis.call('PEXPIRE', KEYS[2], ms)
    end
    return {2, 0}
else
    -- 别人在重建
    return {3, 0}
end