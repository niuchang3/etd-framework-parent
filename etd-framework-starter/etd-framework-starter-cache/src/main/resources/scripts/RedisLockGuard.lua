---
--- Created by 牛昌.
--- DateTime: 2021/7/15 12:12
---


--- 如果Key不存在了，证明已经解锁成功
if (redis.call('exists', KEYS[1]) == 0) then
    return nil;
end
--- 如果不存在则解锁成功
if (redis.call('hexists', KEYS[1], KEYS[2]) == 0) then
    return nil;
end
--- 延长Redis锁过期时间
return redis.call('pexpire', KEYS[1], ARGV[1]);