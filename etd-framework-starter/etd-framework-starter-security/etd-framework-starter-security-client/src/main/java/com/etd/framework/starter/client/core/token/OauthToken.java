package com.etd.framework.starter.client.core.token;

import lombok.Data;

@Data
public class OauthToken {
    /**
     * Token类型
     */
    private String tokenType;
    /**
     * 访问Token
     */
    private String accessToken;
    /**
     * 过期时间
     */
    private Long expires;
    /**
     * 刷新令牌
     */
    private String refreshToken;
    /**
     * 刷新令牌过期时间
     */
    private String refreshExpires;


}
