package com.etd.framework.starter.client.core.constant;

/**
 * 内部登录认证参数常量。
 */
public interface SecurityParameterConstant {


    /**
     * Redis 中保存登录令牌的缓存前缀。
     */
    String TOKEN_CACHE = "TOKEN";

    /**
     * 框架内部支持的令牌类型。
     */
    enum TokenType {
        access_token,
        refresh_token
    }

    /**
     * HTTP 认证方案。
     */
    enum TokenPrompt {
        Bearer
    }


    /**
     * 密码登录
     */
    enum UserPasswordAuthentication {
        username,
        password
    }
}
