local stockKey = KEYS[1]
local holdsKey = KEYS[2]
local accountHoldsKey = KEYS[3]
local holdId = ARGV[1]
local qty = tonumber(ARGV[2])

local userId = redis.call('HGET', holdsKey, holdId)
if not userId then
  return -1
end

redis.call('HDEL', holdsKey, holdId)
local currentAccountCount = tonumber(redis.call('HGET', accountHoldsKey, userId) or '0')
if currentAccountCount > 0 then
  local nextCount = currentAccountCount - 1
  if nextCount <= 0 then
    redis.call('HDEL', accountHoldsKey, userId)
  else
    redis.call('HSET', accountHoldsKey, userId, nextCount)
  end
end

local current = tonumber(redis.call('GET', stockKey) or '0')
local restored = current + qty
redis.call('SET', stockKey, restored)
return restored
