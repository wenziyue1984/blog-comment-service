-- KEYS[1] = CF key
-- ARGV[1] = capacity
-- ARGV[2] = bucketSize or ''       -- 可空，传空串就用默认
-- ARGV[3] = maxIterations or ''    -- 可空
-- ARGV[4] = expansion or ''        -- 可空
-- ARGV[5] = ttlSeconds or ''       -- 可空，传 0/空串则不设置 TTL
--
-- 返回：1=本次创建了过滤器；0=已存在（未创建）

local created = 0
if redis.call('EXISTS', KEYS[1]) == 0 then
    local args = {'CF.RESERVE', KEYS[1], ARGV[1]}
    if ARGV[2] ~= '' then table.insert(args, 'BUCKETSIZE');    table.insert(args, ARGV[2]) end
    if ARGV[3] ~= '' then table.insert(args, 'MAXITERATIONS'); table.insert(args, ARGV[3]) end
    if ARGV[4] ~= '' then table.insert(args, 'EXPANSION');     table.insert(args, ARGV[4]) end
    redis.call(unpack(args))
    if ARGV[5] ~= '' and tonumber(ARGV[5]) > 0 then
        redis.call('EXPIRE', KEYS[1], tonumber(ARGV[5]))
    end
    created = 1
end
return created