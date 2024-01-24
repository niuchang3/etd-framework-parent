package com.etd.framework.starter.oauth.authentication.filter.extend;

import com.etd.framework.starter.oauth.authentication.converter.extend.Oauth2AuthorizeCodeRequestConverter;
import com.etd.framework.starter.oauth.authentication.filter.AbstractOauth2RequestFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Oauth 授权请求过滤器
 * 授权码模式：code 颁发
 * 隐藏式：implicit token颁发
 */
public class Oauth2AuthorizeRequestFilter extends AbstractOauth2RequestFilter {



/*    *//**
     * 默认的身份认证转换器
     *//*
    private void defaultAuthenticationConverter() {
        addAuthenticationConverter(new Oauth2AuthorizeCodeRequestConverter());
    }

    private void defaultRequestMatcher() {
        addAuthenticationRequestMatcher(new AntPathRequestMatcher("/oauth2/authorize"));
    }*/

    @Override
    protected void sendAccessTokenResponse(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

    }

    @Override
    protected void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {

    }
}
