-- KEYS[1] = seq key, like "chat:seq:{sessionId}"
-- ARGV[1] = ttl seconds (string). "0" or "" means no expire update.

local exists = redis.call('EXISTS', KEYS[1])
if exists == 0 then
    return nil
end

local next = redis.call('INCR', KEYS[1])

if ARGV[1] ~= '' then
    local ttl = tonumber(ARGV[1])
    if ttl ~= nil and ttl > 0 then
        redis.call('EXPIRE', KEYS[1], ttl)
    end
end

return next