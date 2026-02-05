-- rotate.lua
-- KEYS[1]=srcKey, KEYS[2]=dstKey
if redis.call('EXISTS', KEYS[1]) == 1 then
    redis.call('RENAME', KEYS[1], KEYS[2])
    return 1
else
    return 0
end