package com.etd.framework.starter.client.core.storage;

import org.etd.framework.starter.cache.RedisCache;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis令牌存储。
 * <p>
 * 只负责令牌数据的Redis读写和动态过期时间，不承载具体登录或OAuth2业务策略。
 */
@Component
public class RedisTokenStore {

    /**
     * 按绝对过期时间保存令牌数据。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param expiresAt 绝对过期时间
     */
    /**
     * put
     *
     * @param key 参数 key
     * @param value 参数 value
     * @param expiresAt 参数 expiresAt
     */
    public void put(String key, Object value, Instant expiresAt) {
        Assert.hasText(key, "令牌缓存键不能为空。");
        Assert.notNull(value, "令牌缓存值不能为空。");
        Assert.notNull(expiresAt, "令牌过期时间不能为空。");
        long expires = Duration.between(Instant.now(), expiresAt).toSeconds();
        if (expires <= 0) {
            throw new IllegalArgumentException("令牌已过期，不能写入存储。");
        }
        RedisCache.set(key, value, expires, TimeUnit.SECONDS);
    }

    /**
     * 获取指定类型的令牌数据。
     *
     * @param key 缓存键
     * @param valueType 缓存值类型
     * @return 缓存值
     * @param <T> 缓存值类型
     */
    /**
     * 获取
     *
     * @param key 参数 key
     * @param valueType 参数 valueType
     * @return 处理结果
     */
    public <T> Optional<T> get(String key, Class<T> valueType) {
        Assert.hasText(key, "令牌缓存键不能为空。");
        Assert.notNull(valueType, "令牌缓存值类型不能为空。");
        Object value = RedisCache.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(valueType.cast(value));
    }

    /**
     * 判断缓存键是否存在。
     *
     * @param key 缓存键
     * @return 是否存在
     */
    /**
     * exists
     *
     * @param key 参数 key
     * @return 处理结果
     */
    public boolean exists(String key) {
        Assert.hasText(key, "令牌缓存键不能为空。");
        return RedisCache.hasKey(key);
    }

    /**
     * 删除指定缓存键。
     *
     * @param keys 缓存键
     */
    public void delete(String... keys) {
        RedisCache.del(keys);
    }

    /**
     * 查询匹配指定模式的缓存键。
     *
     * @param pattern Redis键模式
     * @return 匹配的缓存键
     */
    /**
     * keys
     *
     * @param pattern 参数 pattern
     * @return 处理结果
     */
    public Set<String> keys(String pattern) {
        Assert.hasText(pattern, "令牌缓存键模式不能为空。");
        return RedisCache.getKeys(pattern);
    }
}
