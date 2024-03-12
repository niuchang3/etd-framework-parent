package org.etd.framework.starter.cache.redis.lock;

/**
 * @author 牛昌
 * @version 1.0
 * @date 2021/7/15
 */
public interface RLock {

    /**
     * 加锁
     * @return
     */
    boolean lock();

    /**
     * 开锁
     */
    void unLock();
}
