package com.etd.framework.starter.oauth.authentication.password.configurer;

import com.etd.framework.starter.client.core.AbstractHttpSecurityConfigurer;
import com.etd.framework.starter.oauth.authentication.password.converter.RefreshTokenRequestConverter;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SystemOauthProperties;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationFailureHandler;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationSuccessHandler;
import com.etd.framework.starter.oauth.authentication.password.filter.RefreshTokenRequestFilter;
import com.etd.framework.starter.oauth.authentication.password.provider.RefreshTokenAuthenticationProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;

public class RefreshTokenAuthenticationConfigurer extends AbstractHttpSecurityConfigurer {

    private RequestMatcher authenticationEndpointMatcher;


    public RefreshTokenAuthenticationConfigurer() {
        this(null);
    }

    public RefreshTokenAuthenticationConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
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
            authenticationEndpointMatcher = new AntPathRequestMatcher("/oauth2/refresh", HttpMethod.POST.name());
        }
        builder.authenticationProvider(provider);
    }

    @Override
    public void configure(HttpSecurity builder) {
        ApplicationContext applicationContext = getApplicationContext(builder);
        TokenEncoder tokenEncoder = applicationContext.getBean(TokenEncoder.class);
        SystemOauthProperties oauthProperties = applicationContext.getBean(SystemOauthProperties.class);
        EtdAuthenticationSuccessHandler successHandler = new EtdAuthenticationSuccessHandler(tokenEncoder,oauthProperties);

        RefreshTokenRequestFilter filter = RefreshTokenRequestFilter.builder()
                .refreshTokenEndpointMatcher(authenticationEndpointMatcher)
                .failureHandler(new EtdAuthenticationFailureHandler())
                .successHandler(successHandler)
                .converter(new RefreshTokenRequestConverter())
                .authenticationManager(getAuthenticationManager(builder))
                .build();

        builder.addFilterAfter(filter, UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    public RequestMatcher getRequestMatcher() {
        return null;
    }

    private AuthenticationProvider getAuthenticationProvider(HttpSecurity httpSecurity) {

        TokenDecode tokenDecode = getApplicationContext(httpSecurity).getBean(TokenDecode.class);
        IUserService userService = getApplicationContext(httpSecurity).getBean(IUserService.class);
        return RefreshTokenAuthenticationProvider.builder()
                .tokenDecode(tokenDecode)
                .userService(userService)
                .build();
    }
}
