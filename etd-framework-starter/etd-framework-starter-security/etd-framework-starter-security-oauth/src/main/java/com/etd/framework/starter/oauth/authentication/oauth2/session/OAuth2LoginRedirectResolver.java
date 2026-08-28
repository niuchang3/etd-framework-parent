package com.etd.framework.starter.oauth.authentication.oauth2.session;

import com.etd.framework.starter.oauth.authentication.oauth2.properties.OAuth2SessionProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * OAuth2登录回跳地址解析器。
 */
@RequiredArgsConstructor
public class OAuth2LoginRedirectResolver {

    private final OAuth2SessionProperties properties;

    /**
     * 从内部登录请求中解析OAuth2授权回跳地址。
     *
     * @param request 当前请求
     * @param requestBodyRedirect JSON请求体中的回跳地址
     * @return 合法的回跳地址
     */
    public Optional<String> resolveLoginRedirect(HttpServletRequest request, String requestBodyRedirect) {
        String redirect = StringUtils.hasText(requestBodyRedirect)
                ? requestBodyRedirect
                : request.getParameter(properties.getRedirectParameter());
        if (!isAuthorizeRedirect(redirect)) {
            return Optional.empty();
        }
        return Optional.of(redirect);
    }

    /**
     * 构建未登录时跳转到前端登录页的地址。
     *
     * @param request 当前请求
     * @return 登录页地址
     */
    public String buildLoginRedirect(HttpServletRequest request) {
        String authorizeUrl = getCurrentRequestPath(request);
        return UriComponentsBuilder.fromPath(properties.getLoginPage())
                .queryParam(properties.getRedirectParameter(), authorizeUrl)
                .build()
                .encode()
                .toUriString();
    }

    /**
     * 判断回跳地址是否只指向OAuth2授权端点，避免开放重定向风险。
     *
     * @param redirect 回跳地址
     * @return 是否允许回跳
     */
    private boolean isAuthorizeRedirect(String redirect) {
        if (!StringUtils.hasText(redirect)) {
            return false;
        }
        if (redirect.startsWith("//") || redirect.contains("://")) {
            return false;
        }
        return redirect.equals(properties.getAuthorizeEndpoint())
                || redirect.startsWith(properties.getAuthorizeEndpoint() + "?");
    }

    /**
     * 获取当前请求路径和查询参数，不拼接域名以降低开放重定向风险。
     *
     * @param request 当前请求
     * @return 当前请求相对路径
     */
    private String getCurrentRequestPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (!StringUtils.hasText(queryString)) {
            return requestUri;
        }
        return requestUri + "?" + queryString;
    }
}
