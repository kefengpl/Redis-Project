local key = KEYS[1] -- 获取 key，锁的 key
-- 获取当前线程标识
local threadId = ARGV[1]
-- 获取锁的线程标识
local id = redis.call('get', key)
if (id == threadId)
then
    return redis.call('del', key)
end
return 0