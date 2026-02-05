-- KEYS[1] = bloomSetKey        -- 文章序号集合：SADD/SREM
-- KEYS[2] = bloomBaseKey       -- "bf:pv:<aid>_"
-- KEYS[3] = pvCounterKey       -- "article:pv:count:<aid>"
-- ARGV[1] = fp                 -- murmur 指纹
-- ARGV[2] = initCapacity       -- 1000000
-- ARGV[3] = errorRate          -- 0.01
-- ARGV[4] = threshold          -- 900000
-- ARGV[5] = ttlSeconds         -- 86400

local function bf_key(idx) return KEYS[2] .. idx end

local fp          = ARGV[1]
local capacity    = tonumber(ARGV[2])
local errorRate   = ARGV[3]
local threshold   = tonumber(ARGV[4])
local ttl         = tonumber(ARGV[5])

-- 第一次：建集合 + 第一个桶 + TTL
if redis.call('EXISTS', KEYS[1]) == 0 then
    redis.call('SADD',   KEYS[1], 1)
    redis.call('BF.RESERVE', bf_key(1), errorRate, capacity)
    redis.call('EXPIRE', bf_key(1), ttl)
    redis.call('EXPIRE', KEYS[1],  ttl)
end

-- 获取并排序所有序号
local idxs = redis.call('SMEMBERS', KEYS[1])
table.sort(idxs, function(a,b) return tonumber(a) < tonumber(b) end)

-- 清理已过期的中间桶
for i = #idxs, 1, -1 do
    local k = bf_key(idxs[i])
    if redis.call('EXISTS', k) == 0 then
        redis.call('SREM', KEYS[1], idxs[i])
        table.remove(idxs, i)
    end
end

-- 如果清理后为空，重建第一个桶
if #idxs == 0 then
    redis.call('SADD',   KEYS[1], 1)
    redis.call('BF.RESERVE', bf_key(1), errorRate, capacity)
    redis.call('EXPIRE', bf_key(1), ttl)
    redis.call('EXPIRE', KEYS[1],  ttl)
    idxs = {"1"}
end

local lastIdx = tonumber(idxs[#idxs])
local lastKey = bf_key(lastIdx)

-- 去重 + 计数
local added = redis.call('BF.ADD', lastKey, fp)

if added == 1 then
    redis.call('INCR', KEYS[3])

    local card = redis.call('BF.CARD', lastKey)
    -- RedisBloom < 2.6 没有 BF.CARD，这里兜底成 0
    card = tonumber(card or 0)

    if card >= threshold then
        local newIdx = lastIdx + 1
        local newKey = bf_key(newIdx)
        redis.call('SADD',   KEYS[1], newIdx)
        redis.call('BF.RESERVE', newKey, errorRate, capacity) -- 如需真正“扩容”，此处可改 capacity*2
        redis.call('EXPIRE', newKey, ttl)
        redis.call('EXPIRE', KEYS[1], ttl)
    end
end

return added  -- 1=首次写入；0=已存在