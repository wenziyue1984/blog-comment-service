-- KEYS[1] = 前缀，比如 "blog:comment:like:count_"
-- 生成 64 个哈希：prefix.."00" ... prefix.."63"
-- 若 key 不存在则用 HSET 放一个极小的哨兵字段，存在就跳过
-- 返回：本次新建的分片数量

local prefix = KEYS[1]
local created = 0

for i = 0, 63 do
    local suffix
    if i < 10 then
        suffix = "0" .. i
    else
        suffix = tostring(i)
    end
    local key = prefix .. suffix
    if redis.call('EXISTS', key) == 0 then
        -- 创建一个哈希（Redis 里“空哈希”不存在，必须放一个字段）
        redis.call('HSET', key, '__init__', '1')
        created = created + 1
    end
end

return created