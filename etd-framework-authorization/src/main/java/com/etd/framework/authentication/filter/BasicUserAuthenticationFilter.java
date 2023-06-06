package com.etd.framework.authentication.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
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
public class BasicUserAuthenticationFilter extends OncePerRequestFilter {

    private RequestMatcher authenticationRequestMatcher;

    private AuthenticationManager authenticationManager;

    private AuthenticationSuccessHandler authenticationSuccessHandler = this::doProcessSuccessHandler;

    private AuthenticationFailureHandler authenticationFailureHandler = this::doProcessFailureHandler;


    private AuthenticationConverter converter = null;


    public BasicUserAuthenticationFilter() {
    }

    public BasicUserAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * 添加转换器
     *
     * @param authenticationConverter
     * @return
     */
    public BasicUserAuthenticationFilter setAuthenticationConverter(AuthenticationConverter authenticationConverter) {
        converter = authenticationConverter;
        return this;
    }

    /**
     * 添加认证管理器
     *
     * @param authenticationManager
     * @return
     */
    public BasicUserAuthenticationFilter addAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        return this;
    }

    /**
     * 添加认证请求匹配器
     *
     * @param authenticationRequestMatcher
     * @return
     */
    public BasicUserAuthenticationFilter addAuthenticationRequestMatcher(RequestMatcher authenticationRequestMatcher) {
        this.authenticationRequestMatcher = authenticationRequestMatcher;
        return this;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = converter.convert(request);
        if (ObjectUtils.isEmpty(authentication)) {
            filterChain.doFilter(request, response);
        }
        try {
            Authentication authenticate = authenticationManager.authenticate(authentication);
            doProcessSuccessHandler(request, response, authentication);
        } catch (AuthenticationException e) {
            log.error(e.getMessage(), e);
            doProcessFailureHandler(request, response, e);
        }
    }

    /**
     * 执行成功处理程序
     *
     * @param request
     * @param response
     * @param authentication
     */
    private void doProcessSuccessHandler(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) {
    }

    /**
     * 执行失败处理
     *
     * @param request
     * @param response
     * @param exception
     */
    private void doProcessFailureHandler(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) {

    }

}
