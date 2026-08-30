package com.etd.framework.starter.oauth.authentication.internal.filter;

import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;
import com.etd.framework.starter.client.core.authentication.EtdAuthenticationSuccessHandler;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.storage.UserLoginTokenStorage;
import com.etd.framework.starter.client.core.token.LoginToken;
import com.etd.framework.starter.client.core.token.TokenValue;
import com.etd.framework.starter.oauth.authentication.oauth2.session.OAuth2LoginRedirectResolver;
import org.etd.framework.common.core.user.UserDetails;
import com.etd.framework.starter.oauth.authentication.internal.token.UserPasswordAuthenticationRequestToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

/**
 * 用户密码登录请求过滤器。
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPasswordAuthenticationRequestFilter extends OncePerRequestFilter {


    /**
     * 身份验证管理器。
     */
    private AuthenticationManager authenticationManager;
    /**
     * 登录端点匹配器。
     */
    private RequestMatcher authenticationEndpointMatcher;
    /**
     * 账号密码身份验证转换器。
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
     * 令牌签发器。
     */
    private TokenEncoder<Authentication, TokenValue> tokenEncoder;

    /**
     * 安全认证配置。
     */
    private SecurityProperties securityProperties;

    /**
     * 用户登录令牌存储。
     */
    private UserLoginTokenStorage tokenStorage;

    /**
     * 安全上下文仓储。
     */
    private SecurityContextRepository securityContextRepository;

    /**
     * OAuth2登录回跳地址解析器。
     */
    private OAuth2LoginRedirectResolver oauth2LoginRedirectResolver;

    /**
     * 处理用户密码登录请求。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param filterChain 过滤器链
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!authenticationEndpointMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            UserPasswordAuthenticationRequestToken requestToken = (UserPasswordAuthenticationRequestToken) converter.convert(request);
            Authentication authentication = authenticationManager.authenticate(requestToken);
            onAuthenticationSuccess(request, response, authentication, requestToken.getRedirect());
        } catch (AuthenticationException e) {
            onAuthenticationFailure(request, response, e);
        }
    }

    /**
     * 登录成功后签发访问令牌和刷新令牌。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param authentication 认证结果
     * @param redirect OAuth2授权流程回跳地址
     * @throws ServletException
     * @throws IOException
     */
    private void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication,
                                         String redirect) throws ServletException, IOException {
        Optional<String> oauth2Redirect = resolveOAuth2Redirect(request, redirect);
        if (oauth2Redirect.isPresent()) {
            // OAuth2授权码流程只建立授权中心网页登录态，业务令牌由/oauth2/token后续签发。
            saveSecurityContext(request, response, authentication);
            response.sendRedirect(oauth2Redirect.get());
            return;
        }


        TokenValue accessToken = tokenEncoder.encode(SecurityParameterConstant.TokenType.ACCESS_TOKEN, authentication);
        TokenValue refreshToken = null;
        // 刷新令牌可通过配置关闭，适用于只允许短会话的内部系统。
        if (securityProperties.getRefreshToken() != null && Boolean.TRUE.equals(securityProperties.getRefreshToken().getEnabled())) {
            refreshToken = tokenEncoder.encode(SecurityParameterConstant.TokenType.REFRESH_TOKEN, authentication);
        }
        UserDetails details = (UserDetails) authentication.getDetails();


        LoginToken token = new LoginToken();
        token.setTokenType(SecurityParameterConstant.TokenPrompt.Bearer.name());
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setUserId(String.valueOf(details.getId()));

        // 写入服务端存储，用于后续令牌撤销和单用户登录态覆盖。
        tokenStorage.store(token);
        successHandler.writeBody(response, token);

    }

    /**
     * 解析OAuth2授权流程回跳地址。
     *
     * @param request 当前请求
     * @param redirect 请求体中的回跳地址
     * @return 合法的OAuth2回跳地址
     */
    private Optional<String> resolveOAuth2Redirect(HttpServletRequest request, String redirect) {
        if (oauth2LoginRedirectResolver == null) {
            return Optional.empty();
        }
        return oauth2LoginRedirectResolver.resolveLoginRedirect(request, redirect);
    }

    /**
     * 保存认证结果到SecurityContext。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param authentication 认证结果
     */
    private void saveSecurityContext(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    /**
     * 认证失败时返回统一错误响应。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param exception 认证异常
     * @throws ServletException
     * @throws IOException
     */
    private void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws ServletException, IOException {
        this.logger.trace("认证请求处理失败。", exception);
        this.logger.trace("开始处理认证失败响应。");
        if (exception instanceof AuthenticationServiceException) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        } else {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
        this.failureHandler.onAuthenticationFailure(request, response, exception);
    }

}
