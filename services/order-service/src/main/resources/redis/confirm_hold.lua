local holdsKey = KEYS[1]
local accountHoldsKey = KEYS[2]
local holdId = ARGV[1]
local expectedUserId = ARGV[2]

local userId = redis.call('HGET', holdsKey, holdId)
if not userId then
  return -1
end
if expectedUserId and expectedUserId ~= '' and userId ~= expectedUserId then
  return -2
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

return 1
