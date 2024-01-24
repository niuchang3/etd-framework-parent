package com.etd.framework.starter.oauth.authentication.filter;

import com.etd.framework.starter.oauth.authentication.converter.DelegatingAuthenticationConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Slf4j
public abstract class AbstractOauth2RequestFilter extends OncePerRequestFilter {
    /**
     * 身份认证管理器
     */
    private AuthenticationManager authenticationManager;
    /**
     * 请求参数转换器
     */
    private AuthenticationConverter authenticationConverter;
    /**
     * 认证请求匹配器
     */
    @Getter
    private RequestMatcher requestMatcher;
    /**
     * 成功处理器
     */
    private AuthenticationSuccessHandler successHandler = this::sendAccessTokenResponse;
    /**
     * 失败处理器
     */
    private AuthenticationFailureHandler failureHandler = this::sendErrorResponse;


    private ApplicationContext context;

    /**
     * 添加请求匹配器
     *
     * @param requestMatcher
     */
    public void addAuthenticationRequestMatcher(RequestMatcher requestMatcher) {
        this.requestMatcher = requestMatcher;
    }

    /**
     * 添加身份认证管理器
     *
     * @param authenticationManager
     * @return
     */
    public AbstractOauth2RequestFilter addAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        return this;
    }

    public void addApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    /**
     * 添加身份验证转换器
     *
     * @param authenticationConverter
     */
    public void addAuthenticationConverter(AuthenticationConverter authenticationConverter) {
        if (this.authenticationConverter instanceof DelegatingAuthenticationConverter) {
            ((DelegatingAuthenticationConverter) this.authenticationConverter).addAuthenticationConverter(authenticationConverter);
            return;
        }
        this.authenticationConverter = authenticationConverter;
    }

    /**
     * 添加成功处理器
     *
     * @param successHandler
     * @return
     */
    public AbstractOauth2RequestFilter addSuccessHandler(AuthenticationSuccessHandler successHandler) {
        this.successHandler = successHandler;
        return this;
    }

    /**
     * 添加失败处理器
     *
     * @param failureHandler
     * @return
     */
    public AbstractOauth2RequestFilter addFailureHandler(AuthenticationFailureHandler failureHandler) {
        this.failureHandler = failureHandler;
        return this;
    }

    /**
     * 身份认证业务逻辑
     *
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication authentication = authenticationConverter.convert(request);
        if (ObjectUtils.isEmpty(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            Authentication authenticate = authenticationManager.authenticate(authentication);
            if (!authenticate.isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }
            sendSuccessResponse(request, response, authentication);
        } catch (AuthenticationException e) {
            log.error(e.getMessage(), e);
            sendFailureResponse(request, response, e);
        }

    }

    private void sendSuccessResponse(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        successHandler.onAuthenticationSuccess(request, response, authentication);
    }


    private void sendFailureResponse(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws ServletException, IOException {
        failureHandler.onAuthenticationFailure(request, response, exception);
    }

    /**
     * 认证成功处理
     *
     * @param request
     * @param response
     * @param authentication
     */
    protected abstract void sendAccessTokenResponse(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException;

    /**
     * 认证失败处理
     *
     * @param request
     * @param response
     * @param exception
     */
    protected abstract void sendErrorResponse(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws ServletException, IOException;
}
