package com.etd.framework.starter.oauth.authentication.internal.filter;

import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;
import com.etd.framework.starter.client.core.authentication.EtdAuthenticationSuccessHandler;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import com.etd.framework.starter.client.core.storage.UserLoginTokenStorage;
import com.etd.framework.starter.client.core.token.LoginToken;
import com.etd.framework.starter.client.core.token.TokenValue;
import org.etd.framework.common.core.user.UserDetails;
import com.etd.framework.starter.oauth.authentication.internal.converter.RefreshTokenRequestConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 刷新令牌请求过滤器。
 * <p>
 * 只处理刷新端点请求，认证通过后重新签发访问令牌和刷新令牌。
 */
@Builder
@AllArgsConstructor
public class RefreshTokenRequestFilter extends OncePerRequestFilter {

    /**
     * 身份验证管理器。
     */
    private AuthenticationManager authenticationManager;
    /**
     * 刷新令牌端点匹配器。
     */
    private RequestMatcher refreshTokenEndpointMatcher;
    /**
     * 刷新令牌请求转换器。
     */
    private RefreshTokenRequestConverter converter;


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
     * 处理刷新令牌请求。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param filterChain 过滤器链
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (!refreshTokenEndpointMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Authentication refreshToken = converter.convert(request);
            Authentication authentication = authenticationManager.authenticate(refreshToken);
            onAuthenticationSuccess(response, authentication);
        } catch (AuthenticationException e) {
            onAuthenticationFailure(request, response, e);
        }
    }

    /**
     * 刷新成功后重新签发令牌。
     *
     * @param response 当前响应
     * @param authentication 认证结果
     * @throws ServletException
     * @throws IOException
     */
    private void onAuthenticationSuccess(HttpServletResponse response, Authentication authentication) throws ServletException, IOException {


        TokenValue accessToken = tokenEncoder.encode(SecurityParameterConstant.TokenType.ACCESS_TOKEN, authentication);
        TokenValue refreshToken = null;
        // 刷新令牌开启时采用轮换策略，新的刷新令牌会覆盖旧刷新令牌。
        if (securityProperties.getRefreshToken() != null && Boolean.TRUE.equals(securityProperties.getRefreshToken().getEnabled())) {
            refreshToken = tokenEncoder.encode(SecurityParameterConstant.TokenType.REFRESH_TOKEN, authentication);
        }
        UserDetails details = (UserDetails) authentication.getDetails();
        LoginToken token = new LoginToken();
        token.setTokenType(SecurityParameterConstant.TokenPrompt.Bearer.name());
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setUserId(String.valueOf(details.getId()));
        // 覆盖服务端存储，旧访问令牌和旧刷新令牌会立即失效。
        tokenStorage.store(token);
        successHandler.writeBody(response, token);
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
