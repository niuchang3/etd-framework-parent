package com.etd.framework.starter.oauth.authentication.oauth.filter;

import cn.hutool.core.util.IdUtil;
import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.storage.TokenStorage;
import com.etd.framework.starter.client.core.token.Oauth2AuthorizationCodeRequestToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Oauth 授权验证码过滤器
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Oauth2AuthorizationCodeRequestFilter extends OncePerRequestFilter {

    /**
     * 请求匹配器
     */

    private RequestMatcher requestMatcher;

    /**
     * 登录重定向URL
     */
    private String loginRedirect;
    /**
     * 请求参数转换器
     */
    private AuthenticationConverter converter;

    /**
     * 身份验证管理器
     */
    private AuthenticationManager authenticationManager;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication convert = converter.convert(request);
        SecurityContext context = SecurityContextHolder.getContext();

        //重定向到登录页
        if (ObjectUtils.isEmpty(context.getAuthentication())) {
            redirectLogin(request, response, (Oauth2AuthorizationCodeRequestToken) convert);
            return;
        }
        //开始认证
        Oauth2AuthorizationCodeRequestToken authenticate = null;
        try {
            authenticate = (Oauth2AuthorizationCodeRequestToken) authenticationManager.authenticate(convert);
            if (authenticate.isAuthenticated()) {
                throw new DisabledException("access_denied");
            }
            StringBuffer redirectUri = new StringBuffer(authenticate.getRedirectUri());
            String code = IdUtil.simpleUUID();

            redirectUri.append("?").append(authenticate.getResponseType()).append("=").append(code);
            redirectUri.append("&state=").append(authenticate.getState());


            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            TokenStorage.storage(Oauth2ParameterConstant.TokenNameSpace.AUTHORIZE.name(), code, (String) authentication.getCredentials());
            redirect(redirectUri.toString(), response);

        } catch (AuthenticationException e) {
            onAuthenticationFailure(authenticate, response, e);
        }
    }

    private void onAuthenticationFailure(Oauth2AuthorizationCodeRequestToken requestToken, HttpServletResponse response, AuthenticationException exception) throws ServletException, IOException {
        StringBuffer redirectUrl = new StringBuffer(requestToken.getRedirectUri());
        redirectUrl.append("?error=").append(exception.getMessage()).append("&state=").append(requestToken.getState());
        redirect(redirectUrl.toString(), response);
    }


    private void redirect(String redirectUri, HttpServletResponse response) throws IOException {
        response.sendRedirect(redirectUri);
    }


    private void redirectLogin(HttpServletRequest request, HttpServletResponse response, Oauth2AuthorizationCodeRequestToken requestToken) throws IOException {
        StringBuffer requestURL = new StringBuffer(request.getRequestURL());
        requestURL.append("?response_type=").append(requestToken.getResponseType());
        requestURL.append("&client_id=").append(requestToken.getClientId());
        requestURL.append("&redirect_uri=").append(requestToken.getRedirectUri());
        requestURL.append("&state=").append(requestToken.getState());
        if (!ObjectUtils.isEmpty(requestToken.getScope())) {
            requestURL.append("&scope=").append(String.join(" ", requestToken.getScope()));
        }
        Map<String, String[]> customParameters = requestToken.getCustomParameters();
        if (!CollectionUtils.isEmpty(customParameters)) {
            customParameters.forEach((k, v) -> {
                requestURL.append("&").append(k).append("=");
                if (!ObjectUtils.isEmpty(v)) {
                    requestURL.append(String.join(" ", v));
                }
            });
        }
        Cookie cookie = new Cookie("REDIRECT_URL", URLEncoder.encode(requestURL.toString(), "UTF-8"));
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
        redirect(getLoginRedirect(), response);
    }
}
