package com.etd.framework.starter.client.core.storage;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.token.OauthToken;
import com.etd.framework.starter.client.core.token.OauthTokenNamespace;
import com.etd.framework.starter.client.core.token.OauthTokenValue;
import org.apache.commons.lang3.StringUtils;
import org.etd.framework.starter.cache.RedisCache;
import org.springframework.util.ObjectUtils;

import java.util.Date;

public class TokenStorage {

    private final static String namespace = "namespace";
    /**
     * 获取Token的命名空间
     *
     * @param userId
     * @return
     */
    public static String getTokenNamespace(String userId) {
        return RedisCache.genKey(false, Oauth2ParameterConstant.OAUTH2_TOKEN_CACHE,namespace, userId,namespace);
    }

    /**
     * 获取AccessToken的命名空间
     *
     * @param userId
     * @return
     */
    public static String getAccessTokenNamespace(String userId) {
        String namespace = getTokenNamespace(userId);
        return RedisCache.genKey(false, namespace, Oauth2ParameterConstant.TokenType.access_token.name());
    }

    /**
     * 获取refreshToken的命名空间
     *
     * @param userId
     * @return
     */
    public static String getRefreshTokenNamespace(String userId) {
        String namespace = getTokenNamespace(userId);
        return RedisCache.genKey(false, namespace, Oauth2ParameterConstant.TokenType.refresh_token.name());
    }

    /**
     * 存储
     *
     * @param oauthToken
     */
    public static void storage(OauthToken oauthToken) {
        String tokenNamespace = getTokenNamespace(oauthToken.getUserId());
        String accessTokenNamespace = getAccessTokenNamespace(oauthToken.getUserId());
        String refreshTokenNamespace = getRefreshTokenNamespace(oauthToken.getUserId());


        OauthTokenValue accessToken = oauthToken.getAccessToken();
        OauthTokenValue refreshToken = oauthToken.getRefreshToken();


        Date now = new Date();
        long namespaceExpires = DateUtil.between(now, accessToken.getExpires(), DateUnit.SECOND);

        String userTokenNamespace = RedisCache.genKey(true, tokenNamespace);
        // 封装Token Redis命名空间
        String accessTokenId = RedisCache.genKey(true, accessTokenNamespace, accessToken.getId());
        String refreshTokenId = "";
        if (!ObjectUtils.isEmpty(refreshToken)) {
            refreshTokenId = RedisCache.genKey(true, refreshTokenNamespace, refreshToken.getId());
            namespaceExpires = DateUtil.between(now, refreshToken.getExpires(), DateUnit.SECOND);
        }
        OauthTokenNamespace namespace = OauthTokenNamespace.builder().accessId(accessTokenId).refreshId(refreshTokenId).build();
        RedisCache.set(userTokenNamespace, namespace, namespaceExpires);


        long accessTokenExpires = DateUtil.between(now, accessToken.getExpires(), DateUnit.SECOND);
        RedisCache.set(namespace.getAccessId(), accessToken, accessTokenExpires);
        if (!StringUtils.isEmpty(namespace.getRefreshId())) {
            long refreshTokenExpires = DateUtil.between(now, refreshToken.getExpires(), DateUnit.SECOND);
            RedisCache.set(namespace.getRefreshId(),refreshToken, refreshTokenExpires);
        }
    }

    /**
     * 判断AccessToken是否存在
     *
     * @param userId
     * @param tokenId
     * @return
     */
    public static boolean isExistAccessToken(String userId, String tokenId) {
        String accessTokenNamespace = getAccessTokenNamespace(userId);
        String accessTokenId = RedisCache.genKey(true, accessTokenNamespace, tokenId);
        return RedisCache.hasKey(accessTokenId);
    }

    /**
     * 判断RefreshToken是否存在
     *
     * @param userId
     * @param tokenId
     * @return
     */
    public static boolean isExistRefreshToken(String userId, String tokenId) {
        String refreshTokenNamespace = getRefreshTokenNamespace(userId);
        String refreshTokenId = RedisCache.genKey(true, refreshTokenNamespace, tokenId);
        return RedisCache.hasKey(refreshTokenId);
    }


    public static void delete(Long userId) {
        String namespace = getTokenNamespace(String.valueOf(userId));
        namespace = RedisCache.genKey(true, namespace);
        OauthTokenNamespace tokenNamespace = (OauthTokenNamespace) RedisCache.get(namespace);
        if (ObjectUtils.isEmpty(tokenNamespace)) {
            return;
        }
        RedisCache.del(tokenNamespace.getAccessId());
        RedisCache.del(tokenNamespace.getRefreshId());
        RedisCache.del(namespace);
    }
}
