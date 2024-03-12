package org.etd.framework.starter.cache.redis.lock.config;


import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * @author 牛昌
 * @version 1.0
 * @date 2021/7/15
 */
public class RedisLuaScripts {


    public static final DefaultRedisScript<Long> lock() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/Lock.lua")));
        return redisScript;
    }

    public static final DefaultRedisScript<Long> unLock() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/UnLock.lua")));
        return redisScript;
    }


    public static final DefaultRedisScript<Long> redisLockGuard() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/RedisLockGuard.lua")));
        return redisScript;
    }
}
