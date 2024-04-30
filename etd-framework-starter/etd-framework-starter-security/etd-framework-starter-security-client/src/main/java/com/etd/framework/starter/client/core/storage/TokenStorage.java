package com.etd.framework.starter.client.core.storage;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.token.OauthToken;
import com.etd.framework.starter.client.core.token.OauthTokenValue;
import org.etd.framework.starter.cache.RedisCache;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.Set;

public class TokenStorage {

    private final static String namespace = "namespace";

    /**
     * 获取Token的命名空间
     *
     * @param userId
     * @return
     */
    public static String getTokenNamespace(String tokenNamespace, String userId) {
        return RedisCache.genKey(Oauth2ParameterConstant.OAUTH2_TOKEN_CACHE, namespace, tokenNamespace, userId);
    }

    public static String getAccessTokenNamespace(String tokenNamespace, String userId){
        return RedisCache.genKey(Oauth2ParameterConstant.OAUTH2_TOKEN_CACHE, namespace, tokenNamespace, userId,Oauth2ParameterConstant.TokenType.access_token.name());
    }

    public static String getRefreshTokenNamespace(String tokenNamespace, String userId){
        return RedisCache.genKey(Oauth2ParameterConstant.OAUTH2_TOKEN_CACHE, namespace, tokenNamespace, userId,Oauth2ParameterConstant.TokenType.refresh_token.name());
    }


    /**
     * 存储
     *
     * @param oauthToken
     */
    public static void storage(String nameSpace, OauthToken oauthToken) {

        String accessTokenNamespace = getAccessTokenNamespace(nameSpace, oauthToken.getUserId());
        String refreshTokenNamespace = getRefreshTokenNamespace(nameSpace, oauthToken.getUserId());

        delete(nameSpace,oauthToken.getUserId());


        OauthTokenValue accessToken = oauthToken.getAccessToken();
        Date now = new Date();
        long accessExpires = DateUtil.between(now, accessToken.getExpires(), DateUnit.SECOND);


        RedisCache.set(accessTokenNamespace, accessToken, accessExpires);


        OauthTokenValue refreshToken = oauthToken.getRefreshToken();
        if (ObjectUtils.isEmpty(refreshToken)) {
            return;
        }
        long refreshExpires = DateUtil.between(now, refreshToken.getExpires(), DateUnit.SECOND);

        RedisCache.set(refreshTokenNamespace, refreshToken, refreshExpires);
    }


    public static void storage(String nameSpace, String key, Object value) {
        String tokenNamespace = getTokenNamespace(nameSpace, key);
        RedisCache.set(tokenNamespace, value);
    }

    /**
     * 判断AccessToken是否存在
     *
     * @param nameSpace
     * @param userId
     * @return
     */
    public static boolean isExistAccessToken(String nameSpace, String userId) {
        String accessTokenNamespace = getAccessTokenNamespace(nameSpace, userId);
        return RedisCache.hasKey(accessTokenNamespace);
    }
    /**
     * 判断AccessToken有效性
     *
     * @param nameSpace
     * @param userId
     * @param jwtToken
     * @return
     */
    public static boolean accessMatches(String nameSpace, String userId, String jwtToken) {
        String tokenNamespace = getAccessTokenNamespace(nameSpace, userId);
        OauthTokenValue token = (OauthTokenValue) RedisCache.get(tokenNamespace);
        if (ObjectUtils.isEmpty(token)) {
            return false;
        }
        return token.getValue().equals(jwtToken);
    }

    /**
     * 判断RefreshToken是否存在
     *
     * @param nameSpace
     * @param userId
     * @return
     */
    public static boolean isExistRefreshToken(String nameSpace, String userId) {
        String tokenNamespace = getRefreshTokenNamespace(nameSpace, userId);
        return RedisCache.hasKey(tokenNamespace);
    }

    /**
     * 判断RefreshToken有效性
     *
     * @param nameSpace
     * @param userId
     * @param jwtToken
     * @return
     */
    public static boolean refreshMatches(String nameSpace, String userId, String jwtToken) {
        String tokenNamespace = getRefreshTokenNamespace(nameSpace, userId);
        OauthTokenValue token = (OauthTokenValue) RedisCache.get(tokenNamespace);
        if (ObjectUtils.isEmpty(token)) {
            return false;
        }
        return token.getValue().equals(jwtToken);
    }

    /**
     * 删除Token
     *
     * @param tokenNamespace
     * @param userId
     */
    public static void delete(String tokenNamespace, String userId) {
        String accessTokenNamespace = getAccessTokenNamespace(tokenNamespace, userId);
        String refreshTokenNamespace = getRefreshTokenNamespace(tokenNamespace, userId);
        RedisCache.del(accessTokenNamespace);
        RedisCache.del(refreshTokenNamespace);
    }


    public static void deleteAll(String userId) {
        String tokenNamespace = getTokenNamespace("*", userId);
        Set<String> keys = RedisCache.getKeys(tokenNamespace);
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        String[] array = ArrayUtil.toArray(keys, String.class);
        RedisCache.del(array);
    }

}
