package org.etd.framework.starter.cache.redis.impl;


import lombok.extern.slf4j.Slf4j;
import org.etd.framework.starter.cache.redis.RLock;
import org.etd.framework.starter.cache.redis.RedisLockGuard;
import org.etd.framework.starter.cache.redis.config.RedisLuaScripts;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author 牛昌
 * @version 1.0
 * @date 2021/7/15
 */
@Slf4j
public class RReentrantLock implements RLock {

    /**
     * RedisTemplate
     */
    private RedisTemplate redisTemplate;
    /**
     * 锁的Key值
     */
    private String key;
    /**
     * 过期时长默认 3s
     */
    private long expire = 3000;

    /**
     * 锁前缀
     */
    private static final String prefix = "lock:";
    /**
     * 默认自旋20次，
     */
    private static final int spin = 20;


    private RReentrantLock() {

    }

    public RReentrantLock(RedisTemplate redisTemplate, String key) {
        this.redisTemplate = redisTemplate;
        this.key = key;
    }

    public RReentrantLock(RedisTemplate redisTemplate, String key, long expire) {
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.expire = expire;
    }

    private List<String> getKeys() {
        String threadId = "thread:" + Thread.currentThread().getId();
        String currentKey = prefix + key;
        List<String> keys = new ArrayList<>(Arrays.asList(currentKey));
        keys.add(threadId);
        return keys;
    }


    @Override
    public boolean lock() {
        List<String> keys = getKeys();
        Long execute = 0L;
        int maxSpin = spin;
        int currentSpin = 0;
        while (true) {
            // 如果返回null 则表示获取到锁
            execute = (Long) redisTemplate.execute(RedisLuaScripts.lock(), keys, expire);
            if (execute == null) {
                break;
            }
            if (currentSpin > maxSpin) {
                return false;
            }
            currentSpin++;
            int sleep = (int) (1 + Math.random() * 100);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }
        // 开启守护线程，为Redis锁续命
        Thread thread = new Thread(new RedisLockGuard(keys, expire, redisTemplate, RedisLuaScripts.redisLockGuard()));
        thread.start();
        return true;
    }

    @Override
    public void unLock() {
        List<String> keys = getKeys();
        Long execute = (Long) redisTemplate.execute(RedisLuaScripts.unLock(), keys, expire);
        if (execute == null) {
            throw new RuntimeException("尝试解锁失败，Lock-key:" + key + " ,thread-id:" + Thread.currentThread().getId());
        }
    }


    public static void main(String[] args) {
        int count = 0;
//        for (int i = 0; i < 30; i++) {
//            int sleep = (int) (1 + Math.random() * 100);
//            count = count+sleep;
//            System.out.println(sleep);
//        }
//        System.out.println("睡眠总时长："+count);


        for (int i = 1; i < 51; i++) {
            int sleep = 1 >>> i;
            count = count + sleep;
            System.out.println(sleep);
        }

        System.out.println("共计耗时："+count);

        System.out.println(System.currentTimeMillis() - 1300000000000L);
    }


}
