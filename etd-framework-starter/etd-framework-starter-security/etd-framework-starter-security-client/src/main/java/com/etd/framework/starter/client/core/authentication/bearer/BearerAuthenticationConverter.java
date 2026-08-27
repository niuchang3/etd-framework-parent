package com.etd.framework.starter.client.core.authentication.bearer;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Bearer 请求头认证转换器。
 * <p>
 * 只解析标准的 {@code Authorization: Bearer xxx} 请求头，其他请求直接放行给后续过滤器。
 */
public class BearerAuthenticationConverter implements AuthenticationConverter {

    public static final String AUTHENTICATION_SCHEME_BEARER = "Bearer";

    /**
     * 将请求头中的 Bearer 令牌转换为 Spring Security 认证对象。
     *
     * @param request 当前请求
     * @return Bearer 认证对象，未携带认证头时返回 {@code null}
     */
    @Override
    public Authentication convert(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header)) {
            return null;
        }

        header = header.trim();
        String bearerPrefix = AUTHENTICATION_SCHEME_BEARER + " ";
        if (!StringUtils.startsWithIgnoreCase(header, bearerPrefix)) {
            return null;
        }
        // 只允许 Bearer 后面携带非空令牌，避免把空字符串交给 JWT 解析器。
        String token = header.substring(bearerPrefix.length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BadCredentialsException("认证头令牌不能为空。");
        }

        return new BearerTokenAuthentication(null, token);
    }
}
