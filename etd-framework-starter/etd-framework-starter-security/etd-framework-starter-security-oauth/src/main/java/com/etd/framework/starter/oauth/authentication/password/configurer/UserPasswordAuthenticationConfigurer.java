package com.etd.framework.starter.oauth.authentication.password.configurer;

import com.etd.framework.starter.client.core.AbstractHttpSecurityConfigurer;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SystemOauthProperties;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationFailureHandler;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationSuccessHandler;
import com.etd.framework.starter.oauth.authentication.password.converter.UserPasswordRequestAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.password.filter.UserPasswordAuthenticationRequestFilter;
import com.etd.framework.starter.oauth.authentication.password.provider.UserPasswordAuthenticationProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;


public class UserPasswordAuthenticationConfigurer extends AbstractHttpSecurityConfigurer {

    private RequestMatcher authenticationEndpointMatcher;

    public UserPasswordAuthenticationConfigurer() {
        this(null);
    }


    public UserPasswordAuthenticationConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
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

    @Override
    public void init(HttpSecurity builder) {
        AuthenticationProvider provider = getAuthenticationProvider(builder);
        if (ObjectUtils.isEmpty(authenticationEndpointMatcher)) {
            authenticationEndpointMatcher = new AntPathRequestMatcher("/oauth2/login", HttpMethod.POST.name());
        }
        builder.authenticationProvider(provider);
    }

    @Override
    public void configure(HttpSecurity builder) {
        ApplicationContext applicationContext = getApplicationContext(builder);
        TokenEncoder tokenEncoder = applicationContext.getBean(TokenEncoder.class);
        SystemOauthProperties oauthProperties = applicationContext.getBean(SystemOauthProperties.class);
        EtdAuthenticationSuccessHandler successHandler = new EtdAuthenticationSuccessHandler();

        UserPasswordAuthenticationRequestFilter filter = UserPasswordAuthenticationRequestFilter.builder()
                .authenticationEndpointMatcher(authenticationEndpointMatcher)
                .authenticationManager(getAuthenticationManager(builder))
                .converter(new UserPasswordRequestAuthenticationConverter())
                .successHandler(successHandler)
                .failureHandler(new EtdAuthenticationFailureHandler())
                .tokenEncoder(tokenEncoder)
                .oauthProperties(oauthProperties)
                .build();
        builder.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    public RequestMatcher getRequestMatcher() {
        return authenticationEndpointMatcher;
    }


    private AuthenticationProvider getAuthenticationProvider(HttpSecurity httpSecurity) {
        PasswordEncoder passwordEncoder = getApplicationContext(httpSecurity).getBean(PasswordEncoder.class);
        IUserService userService = getApplicationContext(httpSecurity).getBean(IUserService.class);
        return UserPasswordAuthenticationProvider.builder()
                .passwordEncoder(passwordEncoder)
                .userService(userService)
                .build();
    }
}
