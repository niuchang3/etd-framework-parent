---
--- Created by 牛昌
--- DateTime: 2021/7/15 10:43
---

--- 如果key 已经不存在表示已经解锁成功
if (redis.call('exists', KEYS[1]) == 0) then
    return 1;
end
-- 如果不是当前线程持有，则不能进行解锁
if (redis.call('hexists', KEYS[1], KEYS[2]) == 0) then
    return nil;
end
-- 判断是否为可重入锁，如果是可重入锁value -1
local coun = redis.call('hincrby', KEYS[1], KEYS[2], -1)
if (coun > 0) then
    redis.call('pexpire', KEYS[1], ARGV[1]);
    return 1;
end
--- 如果锁 value ==0 删除 key 全部解锁完成
redis.call('del', KEYS[1]);
return 1;