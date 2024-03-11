package com.etd.framework.starter.client.core.token;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class OauthToken {
    /**
     * Token类型
     */
    private String tokenType;
    /**
     * 访问Token
     */
    private TokenValue accessToken;
    /**
     * 刷新令牌
     */
    private TokenValue refreshToken;

}
