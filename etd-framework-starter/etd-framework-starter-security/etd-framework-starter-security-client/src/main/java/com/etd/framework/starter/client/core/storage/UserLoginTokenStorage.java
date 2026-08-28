package com.etd.framework.starter.client.core.storage;

import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;
import com.etd.framework.starter.client.core.token.LoginToken;
import com.etd.framework.starter.client.core.token.TokenValue;
import lombok.RequiredArgsConstructor;
import org.etd.framework.starter.cache.RedisCache;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * 用户登录令牌存储。
 * <p>
 * 当前策略是单用户只保留一组访问令牌和刷新令牌，新登录或刷新会覆盖旧登录态。
 */
@Component
@RequiredArgsConstructor
public class UserLoginTokenStorage {

    private final RedisTokenStore tokenStore;

    /**
     * 存储用户最新令牌。
     *
     * @param loginToken 登录成功后签发的令牌
     */
    public void store(LoginToken loginToken) {
        validate(loginToken);
        delete(loginToken.getUserId());
        put(getAccessTokenKey(loginToken.getUserId()), loginToken.getAccessToken());
        if (loginToken.getRefreshToken() != null) {
            put(getRefreshTokenKey(loginToken.getUserId()), loginToken.getRefreshToken());
        }
    }

    /**
     * 判断访问令牌是否存在。
     *
     * @param userId 用户标识
     * @return 是否存在
     */
    public boolean isAccessTokenPresent(String userId) {
        return tokenStore.exists(getAccessTokenKey(userId));
    }

    /**
     * 判断访问令牌是否和当前存储值一致。
     *
     * @param userId 用户标识
     * @param tokenValue 当前请求携带的访问令牌
     * @return 是否匹配
     */
    public boolean accessTokenMatches(String userId, String tokenValue) {
        return tokenMatches(getAccessTokenKey(userId), tokenValue);
    }

    /**
     * 判断刷新令牌是否存在。
     *
     * @param userId 用户标识
     * @return 是否存在
     */
    public boolean isRefreshTokenPresent(String userId) {
        return tokenStore.exists(getRefreshTokenKey(userId));
    }

    /**
     * 判断刷新令牌是否和当前存储值一致。
     *
     * @param userId 用户标识
     * @param tokenValue 当前请求携带的刷新令牌
     * @return 是否匹配
     */
    public boolean refreshTokenMatches(String userId, String tokenValue) {
        return tokenMatches(getRefreshTokenKey(userId), tokenValue);
    }

    /**
     * 删除指定用户的访问令牌和刷新令牌。
     *
     * @param userId 用户标识
     */
    public void delete(String userId) {
        tokenStore.delete(getAccessTokenKey(userId), getRefreshTokenKey(userId));
    }

    /**
     * 删除指定用户的全部令牌。
     *
     * @param userId 用户标识
     */
    public void deleteAll(String userId) {
        Set<String> keys = tokenStore.keys(getUserTokenPattern(userId));
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        tokenStore.delete(keys.toArray(String[]::new));
    }

    private void put(String key, TokenValue token) {
        if (!hasValidValue(token)) {
            throw new IllegalArgumentException("令牌值和过期时间不能为空。");
        }
        Instant expiresAt = token.getExpires().toInstant();
        tokenStore.put(key, token, expiresAt);
    }

    private boolean tokenMatches(String key, String tokenValue) {
        Optional<TokenValue> storedToken = tokenStore.get(key, TokenValue.class);
        return storedToken.map(TokenValue::getValue)
                .filter(value -> value.equals(tokenValue))
                .isPresent();
    }

    private boolean hasValidValue(TokenValue token) {
        return token != null
                && !ObjectUtils.isEmpty(token.getValue())
                && token.getExpires() != null;
    }

    private void validate(LoginToken loginToken) {
        if (loginToken == null || ObjectUtils.isEmpty(loginToken.getUserId())) {
            throw new IllegalArgumentException("令牌用户标识不能为空。");
        }
        if (loginToken.getAccessToken() == null) {
            throw new IllegalArgumentException("访问令牌不能为空。");
        }
    }

    private String getAccessTokenKey(String userId) {
        return RedisCache.genKey(SecurityParameterConstant.TOKEN_CACHE, userId,
                SecurityParameterConstant.TokenType.access_token.name());
    }

    private String getRefreshTokenKey(String userId) {
        return RedisCache.genKey(SecurityParameterConstant.TOKEN_CACHE, userId,
                SecurityParameterConstant.TokenType.refresh_token.name());
    }

    private String getUserTokenPattern(String userId) {
        return RedisCache.genKey(SecurityParameterConstant.TOKEN_CACHE, userId, "*");
    }
}
