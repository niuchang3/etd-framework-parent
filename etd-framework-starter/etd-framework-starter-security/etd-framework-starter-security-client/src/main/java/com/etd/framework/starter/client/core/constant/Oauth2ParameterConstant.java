package com.etd.framework.starter.client.core.constant;

/**
 * <a src="https://datatracker.ietf.org/doc/html/rfc6749#section-4.1">OAuth2 文档</a>
 */
public interface Oauth2ParameterConstant {


    String GRANT_TYPE = "grant_type";

    enum TokenType{
        access_token,
        refresh_token
    }

    enum TokenPrompt{
        //BearerTokenAuthenticationFilter
        Bearer
    }


    /**
     * 授权类型
     */
    enum GRANT_TYPE {
        authorization_code,
        client_credentials,
        refresh_token,
        login_password,
        login_mobile,
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
     * 密码登录
     */
    enum UserPasswordAuthentication {
        username,
        password,
//        captcha,

    }

    /**
     * 客户端凭证所需参数
     */
    enum ClientCredentialsAuthentication {
        client_id,
        client_secret,
        grant_type,
        scope
    }
}

