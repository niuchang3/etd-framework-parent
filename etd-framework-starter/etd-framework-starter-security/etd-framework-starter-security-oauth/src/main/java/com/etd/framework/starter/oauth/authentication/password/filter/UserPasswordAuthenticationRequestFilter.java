package com.etd.framework.starter.oauth.authentication.password.filter;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SystemOauthProperties;
import com.etd.framework.starter.client.core.token.OauthToken;
import com.etd.framework.starter.client.core.token.OauthTokenCache;
import com.etd.framework.starter.client.core.token.OauthTokenValue;
import com.etd.framework.starter.client.core.token.UserPasswordAuthenticationRequestToken;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.etd.framework.starter.cache.RedisCache;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户密码身份验证请求过滤器
 * <p>
 * FilterOrderRegistration
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPasswordAuthenticationRequestFilter extends OncePerRequestFilter {


    /**
     * 身份验证管理器
     */
    private AuthenticationManager authenticationManager;
    /**
     * 身份验证端点匹配器
     */
    private RequestMatcher authenticationEndpointMatcher;
    /**
     * 账号密码身份验证转换器
     */
    private AuthenticationConverter converter;


    private AuthenticationSuccessHandler successHandler;

    private AuthenticationFailureHandler failureHandler;

    private TokenEncoder<Authentication, OauthTokenValue> tokenEncoder;

    private SystemOauthProperties oauthProperties;

    /**
     * 过滤器
     *
     * @param request
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!authenticationEndpointMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        UserPasswordAuthenticationRequestToken requestToken = (UserPasswordAuthenticationRequestToken) converter.convert(request);
        try {
            Authentication authentication = authenticationManager.authenticate(requestToken);
            isAuthenticated(authentication);
            // 成功处理
            onAuthenticationSuccess(request, response, authentication);
        } catch (AuthenticationException e) {
            //异常处理
            onAuthenticationFailure(request, response, e);
            throw e;
        }
    }

    /**
     * 认证成功处理
     *
     * @param request
     * @param response
     * @param authentication
     * @throws ServletException
     * @throws IOException
     */
    private void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws ServletException, IOException {

        OauthTokenValue accessToken = tokenEncoder.encode(Oauth2ParameterConstant.TokenType.access_token, authentication);
        OauthTokenValue refreshToken = null;
        if (oauthProperties.getAccessToken().getEnabled()) {
            refreshToken = tokenEncoder.encode(Oauth2ParameterConstant.TokenType.refresh_token, authentication);
        }
        OauthToken token = new OauthToken();
        token.setTokenType(Oauth2ParameterConstant.TokenPrompt.Bearer.name());
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);


        String accessId = RedisCache.genKey(new String(Oauth2ParameterConstant.OAUTH2_CACHE_ACCESS_TOKEN), new String(accessToken.getId()));
        OauthTokenCache oauthTokenCache = OauthTokenCache.builder().accessId(accessId).build();

        Date now = new Date();

        long accessExpires = DateUtil.between(now, accessToken.getExpires(), DateUnit.SECOND);
        if(ObjectUtils.isEmpty(refreshToken)){
            RedisCache.set(accessId,oauthTokenCache,accessExpires);
            successHandler.onAuthenticationSuccess(request, response, token);
            return;
        }
        long refreshExpires = DateUtil.between(now, refreshToken.getExpires(), DateUnit.SECOND);

        String refreshId = RedisCache.genKey(Oauth2ParameterConstant.OAUTH2_CACHE_REFRESH_TOKEN, refreshToken.getId());
        oauthTokenCache.setRefreshId(refreshId);
        RedisCache.set(accessId,oauthTokenCache,accessExpires);
        RedisCache.set(refreshId,oauthTokenCache,refreshExpires);
        successHandler.onAuthenticationSuccess(request, response, token);

    }

    /**
     * 认证失败处理
     *
     * @param request
     * @param response
     * @param exception
     * @throws ServletException
     * @throws IOException
     */
    private void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        this.logger.trace("Failed to process authentication request", exception);
        this.logger.trace("Cleared SecurityContextHolder");
        this.logger.trace("Handling authentication failure");
        this.failureHandler.onAuthenticationFailure(request, response, exception);
    }


    private void isAuthenticated(Authentication authentication) {
        if (!authentication.isAuthenticated()) {
            return;
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

}
