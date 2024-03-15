package com.etd.framework.starter.oauth.authentication.oauth.configurer;

import com.etd.framework.starter.client.core.AbstractHttpSecurityConfigurer;
import com.etd.framework.starter.client.core.filter.BearerTokenAuthenticationFilter;
import com.etd.framework.starter.client.core.oauth.OauthClientService;
import com.etd.framework.starter.oauth.authentication.oauth.converter.Oauth2AuthorizationCodeRequestConverter;
import com.etd.framework.starter.oauth.authentication.oauth.filter.Oauth2AuthorizationCodeRequestFilter;
import com.etd.framework.starter.oauth.authentication.oauth.provider.Oauth2AuthorizationCodeProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;

public class Oauth2AuthorizationCodeConfigurer extends AbstractHttpSecurityConfigurer {

    private RequestMatcher authenticationEndpointMatcher;


    private String loginRedirect;

    public Oauth2AuthorizationCodeConfigurer() {
        this(null);
    }

    public Oauth2AuthorizationCodeConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);
    }

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
            authenticationEndpointMatcher = new AntPathRequestMatcher("/oauth2/authorize", HttpMethod.GET.name());
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
        builder.addFilterBefore(filter, FilterSecurityInterceptor.class);

    }

    @Override
    public RequestMatcher getRequestMatcher() {
        return authenticationEndpointMatcher;
    }
}
