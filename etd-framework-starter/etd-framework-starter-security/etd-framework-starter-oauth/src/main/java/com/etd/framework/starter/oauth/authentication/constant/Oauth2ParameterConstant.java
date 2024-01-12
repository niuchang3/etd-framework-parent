package com.etd.framework.starter.oauth.authentication.constant;

/**
 * <a src="https://datatracker.ietf.org/doc/html/rfc6749#section-4.1">OAuth2 文档</a>
 */
public interface Oauth2ParameterConstant {


    /**
     * 授权类型
     */
    enum GRANT_TYPE {
        authorization_code,
        implicit,
        password,
        client_credentials,
        refresh_token
    }

    /**
     *
     */
    enum ResponseType {
        code,
        token
    }


    /**
     * 授权码认证所需参数
     */
    enum AuthorizationCodeRequestAuthentication {
        response_type,
        client_id,
        redirect_uri,
        scope,
        state
    }


    /**
     * 授权码token所需参数
     */
    enum AuthorizationCodeAuthentication {
        grant_type,
        client_id,
        code,
        redirect_uri
    }

    /**
     * 隐藏式授权所需参数
     */
    enum ImplicitAuthentication {
        response_type,
        client_id,
        redirect_uri,
        scope,
        state,
    }


    /**
     * 密码模式所用参数
     */
    enum PasswordAuthentication {
        grant_type,
        username,
        password,
        scope,
        captcha,

    }

    /**
     * 客户端凭证所需参数
     */
    enum ClientCredentialsAuthentication {
        grant_type,
        scope
    }
}
