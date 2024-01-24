package com.etd.framework.starter.oauth.authentication.filter.extend;

import com.etd.framework.starter.oauth.authentication.converter.extend.OAuth2AuthorizationCodeAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.converter.extend.Oauth2ClientCredentialsAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.filter.AbstractOauth2RequestFilter;
import lombok.Builder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Oauth2 token令牌请求过滤器 负责处理下列请求
 * grant_type：
 * authorization_code
 * password
 * client_credentials
 */
@Builder
public class Oauth2TokenRequestFilter extends AbstractOauth2RequestFilter {


/*    *//**
     * 默认的身份认证转换器
     *//*
    private void defaultAuthenticationConverter() {

        addAuthenticationConverter(new OAuth2AuthorizationCodeAuthenticationConverter());
        addAuthenticationConverter(new Oauth2ClientCredentialsAuthenticationConverter());
    }

    private void defaultRequestMatcher() {
        addAuthenticationRequestMatcher(new AntPathRequestMatcher("/oauth2/token"));
    }*/


    @Override
    protected void sendAccessTokenResponse(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

    }

    @Override
    protected void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {

    }
}
