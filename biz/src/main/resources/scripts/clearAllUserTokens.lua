-- clearAllUserTokens.lua
-- KEYS[1] = 活跃 token 的 Set
-- ARGV[1] = 登录信息缓存前缀(如 "blog:login:userDetail:")
local setKey = KEYS[1]
local prefix = ARGV[1]
if prefix:sub(1,1) == '"' and prefix:sub(-1) == '"' then
  prefix = prefix:sub(2, -2)
end

local members = redis.call('SMEMBERS', setKey)
for _, member in ipairs(members) do
  -- 安全解析 JSON；解析失败时跳过
  local ok, dto = pcall(cjson.decode, member)
  if ok and dto and dto.token then
    --redis.log(redis.LOG_WARNING, prefix)
    --redis.log(redis.LOG_WARNING, dto.token)
    local fullKey = prefix .. dto.token
    --redis.log(redis.LOG_WARNING, "key=" .. fullKey .. ", len=" .. #fullKey .. ", exists=" .. redis.call('EXISTS', fullKey))
    redis.call('DEL', fullKey)
  end
end

-- 最后删掉活跃集合自己
redis.call('DEL', setKey)
return #members   -- 返回清理掉多少条 token