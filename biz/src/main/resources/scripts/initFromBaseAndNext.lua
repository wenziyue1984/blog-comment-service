-- KEYS[1] = seqKey
-- KEYS[2] = rebuildKey
-- ARGV[1] = base (string number)
-- ARGV[2] = ttlSeconds (string)

local base = tonumber(ARGV[1] or '0')
local cur = redis.call('GET', KEYS[1])

if not cur then
    -- 初始化为 base
    redis.call('SET', KEYS[1], tostring(base))
else
    local c = tonumber(cur)
    if c < base then
        -- 发现现值比 base 小（比如过期后被误写小），拉高到 base
        redis.call('SET', KEYS[1], tostring(base))
    end
end

local next = redis.call('INCR', KEYS[1])

local ttl = tonumber(ARGV[2] or '0')
if ttl and ttl > 0 then
    redis.call('EXPIRE', KEYS[1], ttl)
end

-- 清理 rebuild 标记（容错：即使别人清过也没关系）
redis.call('DEL', KEYS[2])

return next