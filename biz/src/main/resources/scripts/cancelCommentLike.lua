-- KEYS[1]=todayCF, KEYS[2]=yesterdayCF, KEYS[3]=hashKey, KEYS[4]=gateKey
-- ARGV[1]=cfItem("commentId:userId"), ARGV[2]=field(commentId), ARGV[3]=delta("-1"), ARGV[4]=shouldDec("1" or "0")

local d1, d2 = 0, 0
if redis.call('EXISTS', KEYS[1]) == 1 then
    d1 = redis.call('CF.DEL', KEYS[1], ARGV[1])
end
if redis.call('EXISTS', KEYS[2]) == 1 then
    d2 = redis.call('CF.DEL', KEYS[2], ARGV[1])
end

local newCnt = nil
if ARGV[4] == '1' then
    newCnt = redis.call('HINCRBY', KEYS[3], ARGV[2], tonumber(ARGV[3]))
    redis.call('DEL', KEYS[4])
end

return {d1, d2, newCnt or -1}