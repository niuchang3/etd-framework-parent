package com.etd.framework.starter.oauth.authentication.internal.filter;

import com.etd.framework.starter.client.core.authentication.EtdAuthenticationSuccessHandler;
import com.etd.framework.starter.client.core.i18n.SecurityMessageCode;
import com.etd.framework.starter.client.core.storage.TokenStorage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 退出登录请求过滤器。
 * <p>
 * 只处理退出端点请求，访问令牌校验通过后删除当前用户服务端令牌。
 */
@Builder
@AllArgsConstructor
public class LogoutRequestFilter extends OncePerRequestFilter {

    /**
     * 身份验证管理器。
     */
    private AuthenticationManager authenticationManager;

    /**
     * 退出登录端点匹配器。
     */
    private RequestMatcher logoutEndpointMatcher;

    /**
     * Bearer 请求头转换器。
     */
    private AuthenticationConverter converter;

    /**
     * 认证成功响应处理器。
     */
    private EtdAuthenticationSuccessHandler successHandler;

    /**
     * 认证失败响应处理器。
     */
    private AuthenticationFailureHandler failureHandler;

    /**
     * 处理退出登录请求。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!logoutEndpointMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Authentication logoutToken = converter.convert(request);
            if (ObjectUtils.isEmpty(logoutToken)) {
                throw new BadCredentialsException(SecurityMessageCode.TOKEN_EMPTY);
            }
            Authentication authentication = authenticationManager.authenticate(logoutToken);
            onAuthenticationSuccess(response, authentication);
        } catch (AuthenticationException e) {
            onAuthenticationFailure(request, response, e);
        }
    }

    /**
     * 退出成功后删除当前用户所有服务端令牌。
     *
     * @param response 当前响应
     * @param authentication 认证结果
     */
    private void onAuthenticationSuccess(HttpServletResponse response, Authentication authentication) throws IOException {
        Object principal = authentication.getPrincipal();
        if (ObjectUtils.isEmpty(principal) || !StringUtils.hasText(String.valueOf(principal))) {
            throw new BadCredentialsException(SecurityMessageCode.TOKEN_INVALID);
        }
        String userId = String.valueOf(principal);
        // 删除访问令牌和刷新令牌，使当前登录态立即失效。
        TokenStorage.deleteAll(userId);
        successHandler.writeBody(response, true);
    }

    /**
     * 认证失败时返回统一错误响应。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param exception 认证异常
     */
    private void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws ServletException, IOException {
        this.logger.trace("退出登录请求处理失败。", exception);
        this.logger.trace("开始处理退出登录失败响应。");
        if (exception instanceof AuthenticationServiceException) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
        this.failureHandler.onAuthenticationFailure(request, response, exception);
    }
}
