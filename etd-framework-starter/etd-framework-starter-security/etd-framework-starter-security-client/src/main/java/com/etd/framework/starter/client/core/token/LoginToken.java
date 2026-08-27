package com.etd.framework.starter.client.core.token;

import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

/**
 * 登录成功后的令牌响应模型。
 * <p>
 * 该对象只用于返回给前端和写入服务端令牌存储，不参与 Spring Security 认证流程。
 */
public class LoginToken {

    /**
     * 用户标识只用于服务端存储令牌，不返回给前端。
     */
    @Getter
    @Setter
    @JsonIgnore
    @JSONField(serialize = false)
    private String userId;
    /**
     * 认证方案。
     */
    @Getter
    @Setter
    private String tokenType;
    /**
     * 访问令牌。
     */
    @Getter
    @Setter
    private TokenValue accessToken;
    /**
     * 刷新令牌。
     */
    @Getter
    @Setter
    private TokenValue refreshToken;
}
