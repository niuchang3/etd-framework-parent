package com.etd.framework.starter.client.core.storage;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;
import com.etd.framework.starter.client.core.token.LoginToken;
import com.etd.framework.starter.client.core.token.TokenValue;
import org.etd.framework.starter.cache.RedisCache;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.Set;

/**
 * 登录令牌服务端存储工具。
 * <p>
 * 当前实现基于 Redis 保存每个用户最新的一组访问令牌和刷新令牌。
 */
public class TokenStorage {

    /**
     * 获取用户访问令牌缓存键。
     *
     * @param userId 用户标识
     * @return Redis 缓存键
     */
    public static String getAccessTokenKey(String userId){
        return RedisCache.genKey(SecurityParameterConstant.TOKEN_CACHE, userId,SecurityParameterConstant.TokenType.access_token.name());
    }

    /**
     * 获取用户刷新令牌缓存键。
     *
     * @param userId 用户标识
     * @return Redis 缓存键
     */
    public static String getRefreshTokenKey(String userId){
        return RedisCache.genKey(SecurityParameterConstant.TOKEN_CACHE, userId,SecurityParameterConstant.TokenType.refresh_token.name());
    }

    private static String getUserTokenPattern(String userId) {
        return RedisCache.genKey(SecurityParameterConstant.TOKEN_CACHE, userId, "*");
    }


    /**
     * 存储用户最新令牌。
     * <p>
     * 当前策略是单用户只保留一组令牌，新登录或刷新会覆盖旧登录态。
     *
     * @param loginToken 登录成功后签发的令牌
     */
    public static void storage(LoginToken loginToken) {
        validToken(loginToken);

        String accessTokenKey = getAccessTokenKey(loginToken.getUserId());
        String refreshTokenKey = getRefreshTokenKey(loginToken.getUserId());

        delete(loginToken.getUserId());


        TokenValue accessToken = loginToken.getAccessToken();
        Date now = new Date();
        long accessExpires = getExpires(now, accessToken);


        RedisCache.set(accessTokenKey, accessToken, accessExpires);


        TokenValue refreshToken = loginToken.getRefreshToken();
        if (ObjectUtils.isEmpty(refreshToken)) {
            return;
        }
        long refreshExpires = getExpires(now, refreshToken);

        RedisCache.set(refreshTokenKey, refreshToken, refreshExpires);
    }

    /**
     * 判断访问令牌是否存在。
     *
     * @param userId 用户标识
     * @return 是否存在
     */
    public static boolean isExistAccessToken(String userId) {
        String accessTokenKey = getAccessTokenKey(userId);
        return RedisCache.hasKey(accessTokenKey);
    }
    /**
     * 判断访问令牌是否和当前存储值一致。
     *
     * @param userId 用户标识
     * @param jwtToken 当前请求携带的访问令牌
     * @return 是否匹配
     */
    public static boolean accessMatches(String userId, String jwtToken) {
        String tokenKey = getAccessTokenKey(userId);
        TokenValue token = (TokenValue) RedisCache.get(tokenKey);
        if (ObjectUtils.isEmpty(token) || ObjectUtils.isEmpty(token.getValue())) {
            return false;
        }
        return token.getValue().equals(jwtToken);
    }

    /**
     * 判断刷新令牌是否存在。
     *
     * @param userId 用户标识
     * @return 是否存在
     */
    public static boolean isExistRefreshToken(String userId) {
        String tokenKey = getRefreshTokenKey(userId);
        return RedisCache.hasKey(tokenKey);
    }

    /**
     * 判断刷新令牌是否和当前存储值一致。
     *
     * @param userId 用户标识
     * @param jwtToken 当前请求携带的刷新令牌
     * @return 是否匹配
     */
    public static boolean refreshMatches(String userId, String jwtToken) {
        String tokenKey = getRefreshTokenKey(userId);
        TokenValue token = (TokenValue) RedisCache.get(tokenKey);
        if (ObjectUtils.isEmpty(token) || ObjectUtils.isEmpty(token.getValue())) {
            return false;
        }
        return token.getValue().equals(jwtToken);
    }

    /**
     * 删除指定用户的登录令牌。
     *
     * @param userId 用户标识
     */
    public static void delete(String userId) {
        String accessTokenKey = getAccessTokenKey(userId);
        String refreshTokenKey = getRefreshTokenKey(userId);
        RedisCache.del(accessTokenKey);
        RedisCache.del(refreshTokenKey);
    }

    /**
     * 删除指定用户的所有令牌。
     * <p>
     * 目前只保留访问令牌和刷新令牌，该方法保留通配删除能力，便于后续扩展令牌类型。
     *
     * @param userId 用户标识
     */
    public static void deleteAll(String userId) {
        String tokenKeyPattern = getUserTokenPattern(userId);
        Set<String> keys = RedisCache.getKeys(tokenKeyPattern);
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        String[] array = ArrayUtil.toArray(keys, String.class);
        RedisCache.del(array);
    }

    private static void validToken(LoginToken loginToken) {
        if (ObjectUtils.isEmpty(loginToken) || ObjectUtils.isEmpty(loginToken.getUserId())) {
            throw new IllegalArgumentException("令牌用户标识不能为空。");
        }
        if (ObjectUtils.isEmpty(loginToken.getAccessToken())) {
            throw new IllegalArgumentException("访问令牌不能为空。");
        }
    }

    /**
     * 计算 Redis 过期秒数。
     *
     * @param now 写入时间
     * @param token 令牌值
     * @return Redis 过期秒数
     */
    private static long getExpires(Date now, TokenValue token) {
        if (ObjectUtils.isEmpty(token.getValue()) || ObjectUtils.isEmpty(token.getExpires())) {
            throw new IllegalArgumentException("令牌值和过期时间不能为空。");
        }
        long expires = DateUtil.between(now, token.getExpires(), DateUnit.SECOND);
        if (expires <= 0) {
            throw new IllegalArgumentException("令牌已过期，不能写入存储。");
        }
        return expires;
    }

}
