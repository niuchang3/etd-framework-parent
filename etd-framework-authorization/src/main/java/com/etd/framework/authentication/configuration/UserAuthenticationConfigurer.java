package com.etd.framework.authentication.configuration;

import com.etd.framework.authentication.AbstractBasicAuthenticationConfigurer;
import com.etd.framework.authentication.converter.DelegatingDefaultAuthenticationConverter;
import com.etd.framework.authentication.converter.UserNamePasswordAuthenticationConverter;
import com.etd.framework.authentication.filter.BasicUserAuthenticationFilter;
import com.etd.framework.authentication.provider.UserNamePasswordAuthenticationProvider;
import com.google.common.collect.Lists;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

/**
 * 用户身份验证配置器
 */
public class UserAuthenticationConfigurer extends AbstractBasicAuthenticationConfigurer {


    private RequestMatcher requestMatcher = new AntPathRequestMatcher("/login", HttpMethod.POST.name());
    /**
     * 认证请求转换器
     */
    private List<AuthenticationConverter> authenticationConverters = defaultAuthenticationConverters();
    /**
     * 身份验证提供程序
     */
    private List<AuthenticationProvider> authenticationProviders = defaultAuthenticationProviders();


    public UserAuthenticationConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);
    }


    public UserAuthenticationConfigurer setRequestMatcher(RequestMatcher requestMatcher) {
        this.requestMatcher = requestMatcher;
        return this;
    }

    public UserAuthenticationConfigurer addAuthenticationConverters(AuthenticationConverter authenticationConverter) {
        authenticationConverters.add(authenticationConverter);
        return this;
    }


    /**
     * 默认的认证请求转换器
     *
     * @return
     */
    private List<AuthenticationConverter> defaultAuthenticationConverters() {
        List<AuthenticationConverter> converters = Lists.newArrayList();
        converters.add(new UserNamePasswordAuthenticationConverter());
        return converters;
    }

    /**
     * 默认的身份验证提供商
     *
     * @return
     */
    private List<AuthenticationProvider> defaultAuthenticationProviders() {
        List<AuthenticationProvider> providers = Lists.newArrayList();
        providers.add(new UserNamePasswordAuthenticationProvider());
        return providers;
    }

    @Override
    public void init(HttpSecurity httpSecurity) {
        authenticationProviders.forEach(provider -> httpSecurity.authenticationProvider(provider));
    }


    @Override
    public void configure(HttpSecurity httpSecurity) {
        BasicUserAuthenticationFilter filter = new BasicUserAuthenticationFilter();
        filter.addAuthenticationManager(httpSecurity.getSharedObject(AuthenticationManager.class))
                .addAuthenticationRequestMatcher(requestMatcher)
                .addAuthenticationConverter(new DelegatingDefaultAuthenticationConverter(authenticationConverters))
                .addApplicationContext(httpSecurity.getSharedObject(ApplicationContext.class));

        httpSecurity.addFilterAfter(filter, FilterSecurityInterceptor.class);
    }

    @Override
    public RequestMatcher getRequestMatcher() {
        return requestMatcher;
    }


}
