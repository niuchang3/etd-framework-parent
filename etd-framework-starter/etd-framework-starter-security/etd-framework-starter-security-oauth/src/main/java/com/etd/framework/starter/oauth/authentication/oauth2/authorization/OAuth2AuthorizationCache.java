package com.etd.framework.starter.oauth.authentication.oauth2.authorization;

import com.etd.framework.starter.client.core.storage.RedisTokenStore;
import lombok.RequiredArgsConstructor;
import org.etd.framework.starter.cache.RedisCache;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * OAuth2授权查询缓存。
 * <p>
 * 只缓存令牌摘要到授权记录标识的索引，完整授权数据仍由OAuth2AuthorizationService持久化，
 * 避免把包含Principal等复杂对象的OAuth2Authorization直接写入Redis。
 */
@Component
@RequiredArgsConstructor
public class OAuth2AuthorizationCache {

    private static final String CACHE_NAME = "OAUTH2_AUTHORIZATION";

    private final RedisTokenStore tokenStore;

    /**
     * 保存令牌到授权记录标识的查询索引。
     *
     * @param tokenType 令牌类型
     * @param tokenValue 令牌原文
     * @param authorizationId 授权记录标识
     * @param expiresAt 令牌过期时间
     */
    public void put(String tokenType, String tokenValue, String authorizationId, Instant expiresAt) {
        Assert.hasText(authorizationId, "OAuth2授权记录标识不能为空。");
        tokenStore.put(buildKey(tokenType, tokenValue), authorizationId, expiresAt);
    }

    /**
     * 根据令牌查询授权记录标识。
     *
     * @param tokenType 令牌类型
     * @param tokenValue 令牌原文
     * @return 授权记录标识
     */
    public Optional<String> findAuthorizationId(String tokenType, String tokenValue) {
        return tokenStore.get(buildKey(tokenType, tokenValue), String.class);
    }

    /**
     * 删除指定令牌对应的授权查询索引。
     *
     * @param tokenType 令牌类型
     * @param tokenValue 令牌原文
     */
    public void evict(String tokenType, String tokenValue) {
        tokenStore.delete(buildKey(tokenType, tokenValue));
    }

    private String buildKey(String tokenType, String tokenValue) {
        Assert.hasText(tokenType, "OAuth2令牌类型不能为空。");
        Assert.hasText(tokenValue, "OAuth2令牌值不能为空。");
        // Redis键中不保存令牌原文，避免运维查询或日志输出时泄露凭证。
        return RedisCache.genKey(CACHE_NAME, tokenType, sha256(tokenValue));
    }

    private String sha256(String tokenValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] tokenDigest = digest.digest(tokenValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(tokenDigest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前JDK不支持SHA-256算法。", exception);
        }
    }
}
