package com.etd.framework.starter.oauth.authentication.internal.factory;

import com.etd.framework.starter.client.core.authentication.bearer.BearerAuthenticationConverter;
import com.etd.framework.starter.oauth.authentication.internal.filter.LogoutRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * 退出登录过滤器工厂。
 * <p>
 * 只接收当前安全链参数，普通 Bean 依赖统一由 {@link AuthenticationFilterSupport} 提供。
 */
@Component
@RequiredArgsConstructor
public class LogoutAuthenticationFilterFactory {

    private final AuthenticationFilterSupport support;

    /**
     * 创建退出登录过滤器。
     *
     * @param logoutEndpointMatcher 退出登录端点匹配器
     * @param authenticationManager 当前安全链认证管理器
     * @return 退出登录过滤器
     */
    public LogoutRequestFilter create(RequestMatcher logoutEndpointMatcher,
                                      AuthenticationManager authenticationManager) {
        return LogoutRequestFilter.builder()
                .logoutEndpointMatcher(logoutEndpointMatcher)
                .authenticationManager(authenticationManager)
                .converter(new BearerAuthenticationConverter())
                .successHandler(support.successHandler())
                .failureHandler(support.failureHandler())
                .tokenStorage(support.tokenStorage())
                .build();
    }
}
