package com.etd.framework.starter.oauth.authentication.configurer.extend;

import com.etd.framework.starter.oauth.authentication.configurer.AbstractAuthorizationConfigurer;
import com.etd.framework.starter.oauth.authentication.converter.DelegatingAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.converter.extend.LoginAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.filter.extend.LoginAuthorizeRequestFilter;
import com.etd.framework.starter.oauth.authentication.provider.LoginAuthenticationProvider;
import com.etd.framework.starter.oauth.authentication.service.UserPasswordService;
import com.google.common.collect.Lists;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;
import java.util.function.Consumer;

public class LoginAuthorizationConfigurer extends AbstractAuthorizationConfigurer {
    /**
     * 请求匹配
     */
    private RequestMatcher loginRequestMatcher;
    /**
     * 登录请求转换器
     */
    private Consumer<List<AuthenticationConverter>> loginRequestConverterConsumer = (authenticationConverters) -> {
    };


    private Consumer<List<AuthenticationProvider>> authenticationProvidersConsumer = (authenticationProviders) -> {
    };


    public LoginAuthorizationConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);
    }

    /**
     * 添加登录请求参数转换器
     *
     * @param authenticationConverters
     * @return
     */
    public LoginAuthorizationConfigurer addLoginRequestConverter(Consumer<List<AuthenticationConverter>> authenticationConverters) {
        this.loginRequestConverterConsumer = authenticationConverters;
        return this;
    }

    /**
     * 添加身份认证提供程序
     *
     * @param authenticationProviders
     * @return
     */
    public LoginAuthorizationConfigurer addAuthenticationProvider(Consumer<List<AuthenticationProvider>> authenticationProviders) {
        this.authenticationProvidersConsumer = authenticationProviders;
        return this;
    }

    @Override
    public void init(HttpSecurity httpSecurity) {
        loginRequestMatcher = new AntPathRequestMatcher("/oauth2/login", HttpMethod.POST.name());
        getAuthenticationProvider(httpSecurity).forEach(postProcess(httpSecurity::authenticationProvider));
    }

    @Override
    public void configure(HttpSecurity httpSecurity) {

        AuthenticationConverter converter = getAuthenticationConverter();
        AuthenticationManager authenticationManager = getAuthenticationManager(httpSecurity);
        ApplicationContext applicationContext = getApplicationContext(httpSecurity);

        //普通登录请求
        LoginAuthorizeRequestFilter loginAuthorizeRequestFilter = new LoginAuthorizeRequestFilter();
        loginAuthorizeRequestFilter.addAuthenticationRequestMatcher(getRequestMatcher());
        loginAuthorizeRequestFilter.addAuthenticationConverter(converter);
        loginAuthorizeRequestFilter.addAuthenticationManager(authenticationManager);
        loginAuthorizeRequestFilter.addApplicationContext(applicationContext);
        httpSecurity.addFilterAfter(postProcess(loginAuthorizeRequestFilter), AbstractPreAuthenticatedProcessingFilter.class);
    }


    @Override
    public RequestMatcher getRequestMatcher() {
        return loginRequestMatcher;
    }


    /**
     * 默认的认证请求转换器
     *
     * @return
     */
    private List<AuthenticationConverter> defaultAuthenticationConverter() {
        List<AuthenticationConverter> converter = Lists.newArrayList();
        converter.add(new LoginAuthenticationConverter());
        return converter;
    }

    /**
     * 获取认证请求转换器
     *
     * @return
     */
    private AuthenticationConverter getAuthenticationConverter() {
        List<AuthenticationConverter> converters = defaultAuthenticationConverter();
        loginRequestConverterConsumer.accept(converters);
        DelegatingAuthenticationConverter delegatingConverter = new DelegatingAuthenticationConverter();
        delegatingConverter.addAuthenticationConverter(converters);
        return delegatingConverter;
    }

    /**
     * 默认的身份认证提供程序
     *
     * @return
     */
    private List<AuthenticationProvider> defaultAuthenticationProvider(HttpSecurity httpSecurity) {
        List<AuthenticationProvider> authenticationProviders = Lists.newArrayList();
        UserPasswordService userPasswordService = getApplicationContext(httpSecurity).getBean(UserPasswordService.class);
        authenticationProviders.add(new LoginAuthenticationProvider(userPasswordService));
        return authenticationProviders;
    }

    /**
     * 获取身份认证提供程序
     *
     * @return
     */
    private List<AuthenticationProvider> getAuthenticationProvider(HttpSecurity httpSecurity) {

        List<AuthenticationProvider> providers = defaultAuthenticationProvider(httpSecurity);
        authenticationProvidersConsumer.accept(providers);
        return providers;
    }

}
