local stockKey = KEYS[1]
local holdsKey = KEYS[2]
local accountHoldsKey = KEYS[3]
local holdId = ARGV[1]
local userId = ARGV[2]
local qty = tonumber(ARGV[3])
local ttlMs = tonumber(ARGV[4])
local maxPerAccount = tonumber(ARGV[5])

if maxPerAccount and maxPerAccount > 0 then
  local activeHolds = tonumber(redis.call('HGET', accountHoldsKey, userId) or '0')
  if activeHolds >= maxPerAccount then
    return -2
  end
end

local available = tonumber(redis.call('GET', stockKey) or '0')
if available < qty then
  return -1
end

local remaining = available - qty
redis.call('SET', stockKey, remaining)
redis.call('HSET', holdsKey, holdId, userId)
redis.call('HINCRBY', accountHoldsKey, userId, 1)
redis.call('PEXPIRE', holdsKey, ttlMs)
return remaining
