package com.etd.framework.starter.oauth.authentication.filter.extend;

import com.etd.framework.starter.oauth.authentication.filter.AbstractOauth2RequestFilter;
import lombok.Builder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Oauth2 token令牌请求过滤器 负责处理下列请求
 * grant_type：
 * authorization_code
 * password
 * client_credentials
 */
@Builder
public class Oauth2TokenRequestFilter extends AbstractOauth2RequestFilter {


    /**
     * 默认的Oauth2Token请求配置
     */
    public Oauth2TokenRequestFilter() {
        defaultRequestMatcher();
        defaultAuthenticationConverter();
    }

    /**
     * 默认的身份认证转换器
     */
    private void defaultAuthenticationConverter() {
        addAuthenticationConverter(null);
    }

    private void defaultRequestMatcher() {
        addAuthenticationRequestMatcher(new AntPathRequestMatcher("/oauth2/token"));
    }

}
