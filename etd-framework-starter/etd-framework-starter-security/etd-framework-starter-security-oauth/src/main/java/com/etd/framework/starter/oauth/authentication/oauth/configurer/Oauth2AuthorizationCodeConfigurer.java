package com.etd.framework.starter.oauth.authentication.oauth.configurer;

import com.etd.framework.starter.client.core.AbstractHttpSecurityConfigurer;
import com.etd.framework.starter.client.core.filter.BearerTokenAuthenticationFilter;
import com.etd.framework.starter.client.core.oauth.OauthClientService;
import com.etd.framework.starter.oauth.authentication.oauth.converter.Oauth2AuthorizationCodeRequestConverter;
import com.etd.framework.starter.oauth.authentication.oauth.filter.Oauth2AuthorizationCodeRequestFilter;
import com.etd.framework.starter.oauth.authentication.oauth.provider.Oauth2AuthorizationCodeProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;

public class Oauth2AuthorizationCodeConfigurer extends AbstractHttpSecurityConfigurer {

    private static final PathPatternRequestMatcher.Builder PATH_MATCHER = PathPatternRequestMatcher.withDefaults();

    private RequestMatcher authenticationEndpointMatcher;


    private String loginRedirect;

    /**
     * 外部 添加请求匹配端点
     *
     * @param requestMatcher
     */
    public void addEndpointMatcher(RequestMatcher requestMatcher) {
        this.authenticationEndpointMatcher = requestMatcher;
    }

    /**
     * 外部 添加请求匹配端点
     *
     * @param loginRedirect
     */
    public void addLoginRedirect(String loginRedirect) {
        this.loginRedirect = loginRedirect;
    }


    @Override
    public void init(HttpSecurity builder) {
        OauthClientService bean = getApplicationContext(builder).getBean(OauthClientService.class);
        Oauth2AuthorizationCodeProvider provider = Oauth2AuthorizationCodeProvider.builder().oauthClientService(bean).build();
        builder.authenticationProvider(provider);

        if (ObjectUtils.isEmpty(authenticationEndpointMatcher)) {
            // 默认授权码登录端点。
            authenticationEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.GET, "/oauth2/authorize");
        }
        if(ObjectUtils.isEmpty(loginRedirect)){
            loginRedirect = "http://127.0.0.1:7000/login";
        }
    }

    @Override
    public void configure(HttpSecurity builder) {
        Oauth2AuthorizationCodeRequestFilter filter = new Oauth2AuthorizationCodeRequestFilter();
        filter.setRequestMatcher(authenticationEndpointMatcher);
        filter.setLoginRedirect(loginRedirect);
        filter.setConverter(new Oauth2AuthorizationCodeRequestConverter());
        filter.setAuthenticationManager(getAuthenticationManager(builder));
        builder.addFilterBefore(filter, AuthorizationFilter.class);

    }

    @Override
    public RequestMatcher getRequestMatcher() {
        return authenticationEndpointMatcher;
    }
}
