package com.etd.framework.starter.client.core.authentication.bearer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Bearer 令牌认证过滤器。
 * <p>
 * 每次请求都会尝试从请求头解析令牌；未携带令牌时放行，携带令牌时交给认证管理器校验。
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {


    /**
     * 请求到认证对象的转换器。
     */
    private AuthenticationConverter converter;

    /**
     * Spring Security 认证管理器。
     */
    private AuthenticationManager authenticationManager;

    /**
     * 认证失败响应处理器。
     */
    private AuthenticationFailureHandler failureHandler;


    /**
     * 解析并认证 Bearer 令牌。
     *
     * @param request     当前请求
     * @param response    当前响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean contextSet = false;
        try {
            Authentication convert = converter.convert(request);
            if (convert == null) {
                filterChain.doFilter(request, response);
                return;
            }
            Authentication authenticate = authenticationManager.authenticate(convert);
            if (!authenticate.isAuthenticated()) {
                throw new CredentialsExpiredException("凭证错误");
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authenticate);
            SecurityContextHolder.setContext(context);
            contextSet = true;
            filterChain.doFilter(request, response);
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            failureHandler.onAuthenticationFailure(request, response, exception);
        } finally {
            // 令牌认证的身份只在当前请求内有效，请求链结束后清理线程上下文。
            if (contextSet) {
                SecurityContextHolder.clearContext();
            }
        }

    }

}
