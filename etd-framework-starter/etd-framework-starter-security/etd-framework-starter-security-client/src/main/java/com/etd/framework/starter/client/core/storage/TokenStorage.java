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
    public static String getTokenNamespace(String tokenNamespace,String userId) {
        return RedisCache.genKey(false, Oauth2ParameterConstant.OAUTH2_TOKEN_CACHE,namespace, userId,tokenNamespace);
    }

    /**
     * 获取AccessToken的命名空间
     *
     * @param userId
     * @return
     */
    public static String getAccessTokenNamespace(String tokenNamespace,String userId) {
        String namespace = getTokenNamespace(tokenNamespace,userId);
        return RedisCache.genKey(false, namespace, Oauth2ParameterConstant.TokenType.access_token.name());
    }

    /**
     * 获取refreshToken的命名空间
     *
     * @param userId
     * @return
     */
    public static String getRefreshTokenNamespace(String tokenNamespace,String userId) {
        String namespace = getTokenNamespace(tokenNamespace,userId);
        return RedisCache.genKey(false, namespace, Oauth2ParameterConstant.TokenType.refresh_token.name());
    }

    /**
     * 存储
     *
     * @param oauthToken
     */
    public static void storage(String tokenNameSpace,OauthToken oauthToken) {
        delete(tokenNameSpace,oauthToken.getUserId());
        String tokenNamespace = getTokenNamespace(tokenNameSpace,oauthToken.getUserId());
        String accessTokenNamespace = getAccessTokenNamespace(tokenNameSpace,oauthToken.getUserId());
        String refreshTokenNamespace = getRefreshTokenNamespace(tokenNameSpace,oauthToken.getUserId());


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
    public static boolean isExistAccessToken(String tokenNameSpace,String userId, String tokenId) {
        String accessTokenNamespace = getAccessTokenNamespace(tokenNameSpace,userId);
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
    public static boolean isExistRefreshToken(String tokenNameSpace,String userId, String tokenId) {
        String refreshTokenNamespace = getRefreshTokenNamespace(tokenNameSpace,userId);
        String refreshTokenId = RedisCache.genKey(true, refreshTokenNamespace, tokenId);
        return RedisCache.hasKey(refreshTokenId);
    }


    public static void delete(String tokenNamespace,Long userId) {
        delete(tokenNamespace,String.valueOf(userId));
    }

    public static void delete(String tokenNamespace,String userId) {
        String namespace = getTokenNamespace(tokenNamespace,userId);
        namespace = RedisCache.genKey(true,namespace);

        OauthTokenNamespace bean = (OauthTokenNamespace) RedisCache.get(namespace);

        if (ObjectUtils.isEmpty(bean)) {
            return;
        }
        RedisCache.del(bean.getAccessId());
        RedisCache.del(bean.getRefreshId());
        RedisCache.del(namespace);
    }
}
