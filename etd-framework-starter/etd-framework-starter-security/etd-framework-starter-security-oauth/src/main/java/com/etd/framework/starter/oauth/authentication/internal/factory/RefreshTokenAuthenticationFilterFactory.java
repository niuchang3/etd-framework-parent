package com.etd.framework.starter.oauth.authentication.internal.factory;

import com.etd.framework.starter.oauth.authentication.internal.filter.RefreshTokenRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * 刷新令牌过滤器工厂。
 * <p>
 * 只接收当前安全链参数，普通 Bean 依赖统一由 {@link AuthenticationFilterSupport} 提供。
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenAuthenticationFilterFactory {

    private final AuthenticationFilterSupport support;

    /**
     * 创建刷新令牌过滤器。
     *
     * @param refreshTokenEndpointMatcher 刷新令牌端点匹配器
     * @param authenticationManager 当前安全链认证管理器
     * @return 刷新令牌过滤器
     */
    public RefreshTokenRequestFilter create(RequestMatcher refreshTokenEndpointMatcher,
                                            AuthenticationManager authenticationManager) {
        return RefreshTokenRequestFilter.builder()
                .refreshTokenEndpointMatcher(refreshTokenEndpointMatcher)
                .authenticationManager(authenticationManager)
                .converter(support.refreshTokenConverter())
                .successHandler(support.successHandler())
                .failureHandler(support.failureHandler())
                .tokenEncoder(support.tokenEncoder())
                .securityProperties(support.securityProperties())
                .build();
    }
}
