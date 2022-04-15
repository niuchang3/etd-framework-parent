---
---Redis分布式锁
--- Created by 牛昌.
--- DateTime: 2021/7/14 14:53
---
---


--- 如果Key不存在，直接加锁，并设置过期时长，过期时长单位为毫秒数
if (redis.call('exists', KEYS[1]) == 0) then
    redis.call('hset', KEYS[1], KEYS[2], 1);
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return nil;
end
--- 如果 key存在，判断是否为当前线程持有
--- 如果是当前线程持有，则调整为可重入锁
--- 对当前锁持有 + 1 并重置过期时长
if (redis.call('hexists', KEYS[1], KEYS[2]) == 1) then
    redis.call('hincrby', KEYS[1], KEYS[2], 1);
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return nil;
end

--- 如果Key存在，并且不是当前线程持有则返回过期时长
return redis.call('pttl', KEYS[1]);


