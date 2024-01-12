package com.etd.framework.starter.oauth.authentication.filter;

import com.etd.framework.starter.oauth.authentication.converter.DelegatingAuthenticationConverter;
import lombok.Getter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public abstract class AbstractOauth2RequestFilter extends OncePerRequestFilter {
    /**
     * 身份认证管理器
     */
    @Getter
    private AuthenticationManager authenticationManager;
    /**
     * 认证请求匹配器
     */
    @Getter
    private RequestMatcher requestMatcher;
    /**
     * 请求参数转换器
     */
    @Getter
    private AuthenticationConverter authenticationConverter = new DelegatingAuthenticationConverter();


    /**
     * 设置身份验证匹配器
     *
     * @param requestMatcher
     * @return
     */
    public AbstractOauth2RequestFilter addAuthenticationRequestMatcher(RequestMatcher requestMatcher) {
        this.requestMatcher = requestMatcher;
        return this;
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


    public AbstractOauth2RequestFilter addAuthenticationConverter(AuthenticationConverter authenticationConverter) {
        ((DelegatingAuthenticationConverter) this.authenticationConverter).addAuthenticationConverter(authenticationConverter);
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


//        authenticationManager.authenticate()
//        authenticationManager.authenticate()

    }


}
