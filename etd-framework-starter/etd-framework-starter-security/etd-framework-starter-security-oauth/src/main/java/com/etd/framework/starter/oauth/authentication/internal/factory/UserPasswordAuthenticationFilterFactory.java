package com.etd.framework.starter.oauth.authentication.internal.factory;

import com.etd.framework.starter.oauth.authentication.internal.filter.UserPasswordAuthenticationRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * 用户密码登录过滤器工厂。
 * <p>
 * 只接收当前安全链参数，普通 Bean 依赖统一由 {@link AuthenticationFilterSupport} 提供。
 */
@Component
@RequiredArgsConstructor
public class UserPasswordAuthenticationFilterFactory {

    private final AuthenticationFilterSupport support;

    /**
     * 创建用户密码登录过滤器。
     *
     * @param authenticationEndpointMatcher 登录端点匹配器
     * @param authenticationManager 当前安全链认证管理器
     * @return 用户密码登录过滤器
     */
    public UserPasswordAuthenticationRequestFilter create(RequestMatcher authenticationEndpointMatcher,
                                                          AuthenticationManager authenticationManager) {
        return UserPasswordAuthenticationRequestFilter.builder()
                .authenticationEndpointMatcher(authenticationEndpointMatcher)
                .authenticationManager(authenticationManager)
                .converter(support.userPasswordConverter())
                .successHandler(support.successHandler())
                .failureHandler(support.failureHandler())
                .tokenEncoder(support.tokenEncoder())
                .securityProperties(support.securityProperties())
                .build();
    }
}
