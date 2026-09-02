package com.etd.framework.starter.client.core.authentication;

import com.etd.framework.starter.client.core.i18n.SecurityMessageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 认证成功响应处理器。
 * <p>
 * 登录和刷新令牌成功后，统一把认证对象包装为框架响应体。
 */
@Component
public class EtdAuthenticationSuccessHandler extends AbstractAuthenticationHandler implements AuthenticationSuccessHandler {

    public EtdAuthenticationSuccessHandler(ObjectMapper objectMapper, SecurityMessageResolver securityMessageResolver) {
        super(objectMapper, securityMessageResolver);
    }

    /**
     * 写出认证成功响应。
     *
     * @param request        当前请求
     * @param response       当前响应
     * @param authentication 认证成功后的对象
     */
    /**
     * on Authentication Success
     *
     * @param request 参数 request
     * @param response 参数 response
     * @param authentication 参数 authentication
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        writeSuccess(response, ResultModel.success(authentication));
    }

    /**
     * 写出普通响应对象。
     * <p>
     * 登录和刷新令牌端点返回的是令牌响应模型，不应该强行实现 {@link Authentication}。
     *
     * @param response 当前响应
     * @param body 响应数据
     */
    /**
     * write Body
     *
     * @param response 参数 response
     * @param body 参数 body
     */
    public void writeBody(HttpServletResponse response, Object body) throws IOException {
        writeSuccess(response, ResultModel.success(body));
    }
}
