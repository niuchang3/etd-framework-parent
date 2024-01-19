package com.etd.framework.starter.oauth.authentication.filter.extend;

import com.etd.framework.starter.oauth.authentication.converter.extend.Oauth2AuthorizeCodeRequestConverter;
import com.etd.framework.starter.oauth.authentication.converter.extend.Oauth2ImplicitAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.filter.AbstractOauth2RequestFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Oauth 授权请求过滤器
 * 授权码模式：code 颁发
 * 隐藏式：implicit token颁发
 */
public class Oauth2AuthorizeRequestFilter extends AbstractOauth2RequestFilter {


    public Oauth2AuthorizeRequestFilter() {
        defaultRequestMatcher();
        defaultAuthenticationConverter();
    }


    /**
     * 默认的身份认证转换器
     */
    private void defaultAuthenticationConverter() {
        addAuthenticationConverter(new Oauth2AuthorizeCodeRequestConverter());
        addAuthenticationConverter(new Oauth2ImplicitAuthenticationConverter());
    }

    private void defaultRequestMatcher() {
        addAuthenticationRequestMatcher(new AntPathRequestMatcher("/oauth2/authorize"));
    }
}
