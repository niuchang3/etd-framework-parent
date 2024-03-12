package org.etd.framework.starter.cache.redis.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * @author 牛昌
 * @version 1.0
 * @date 2021/7/15
 */
@Slf4j
public class RedisLockGuard implements Runnable {



    private List<String> keys;

    private long ttl;

    private RedisTemplate redisTemplate;

    private DefaultRedisScript<Long> redisLockGuard;

    public RedisLockGuard(List<String> keys, long ttl, RedisTemplate redisTemplate, DefaultRedisScript<Long> redisLockGuard) {
        this.keys = keys;
        this.ttl = ttl;
        this.redisTemplate = redisTemplate;
        this.redisLockGuard = redisLockGuard;
    }

    @Override
    public void run() {
        Long execute = 0L;
        while (true) {
            execute = (Long) redisTemplate.execute(redisLockGuard, keys, ttl);
            if (execute == null) {
                log.debug("退出RedisKey守护线程：" + keys.toString());
                return;
            }
            try {
                Thread.sleep(ttl / 2);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
