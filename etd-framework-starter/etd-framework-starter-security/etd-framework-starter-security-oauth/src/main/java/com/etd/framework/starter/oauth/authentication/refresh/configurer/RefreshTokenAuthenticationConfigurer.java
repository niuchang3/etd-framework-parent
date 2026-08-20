package com.etd.framework.starter.oauth.authentication.refresh.configurer;

import com.etd.framework.starter.client.core.AbstractHttpSecurityConfigurer;
import com.etd.framework.starter.oauth.authentication.refresh.converter.RefreshTokenRequestConverter;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SystemOauthProperties;
import com.etd.framework.starter.client.core.user.IUserService;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationFailureHandler;
import com.etd.framework.starter.oauth.authentication.EtdAuthenticationSuccessHandler;
import com.etd.framework.starter.oauth.authentication.refresh.filter.RefreshTokenRequestFilter;
import com.etd.framework.starter.oauth.authentication.refresh.provider.RefreshTokenAuthenticationProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;

public class RefreshTokenAuthenticationConfigurer extends AbstractHttpSecurityConfigurer {

    private static final PathPatternRequestMatcher.Builder PATH_MATCHER = PathPatternRequestMatcher.withDefaults();

    private RequestMatcher authenticationEndpointMatcher;

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
            // 默认刷新 token 端点。
            authenticationEndpointMatcher = PATH_MATCHER.matcher(HttpMethod.POST, "/oauth2/refresh");
        }
        builder.authenticationProvider(provider);
    }

    @Override
    public void configure(HttpSecurity builder) {
        ApplicationContext applicationContext = getApplicationContext(builder);
        TokenEncoder tokenEncoder = applicationContext.getBean(TokenEncoder.class);
        SystemOauthProperties oauthProperties = applicationContext.getBean(SystemOauthProperties.class);


        EtdAuthenticationSuccessHandler successHandler = new EtdAuthenticationSuccessHandler();

        RefreshTokenRequestFilter filter = RefreshTokenRequestFilter.builder()
                .refreshTokenEndpointMatcher(authenticationEndpointMatcher)
                .failureHandler(new EtdAuthenticationFailureHandler())
                .successHandler(successHandler)
                .converter(new RefreshTokenRequestConverter())
                .authenticationManager(getAuthenticationManager(builder))
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

        TokenDecode tokenDecode = getApplicationContext(httpSecurity).getBean(TokenDecode.class);
        IUserService userService = getApplicationContext(httpSecurity).getBean(IUserService.class);
        return RefreshTokenAuthenticationProvider.builder()
                .tokenDecode(tokenDecode)
                .userService(userService)
                .build();
    }
}
