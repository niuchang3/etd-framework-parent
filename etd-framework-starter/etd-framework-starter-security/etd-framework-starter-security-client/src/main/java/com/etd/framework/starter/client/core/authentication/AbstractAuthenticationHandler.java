package com.etd.framework.starter.client.core.authentication;

import com.etd.framework.starter.client.core.i18n.SecurityMessageResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 认证响应处理器基类。
 * <p>
 * 统一处理 JSON 序列化、响应状态码和国际化消息解析，避免各个安全处理器重复写响应。
 */
@Slf4j
public abstract class AbstractAuthenticationHandler {
    private final ObjectMapper objectMapper;
    private final SecurityMessageResolver securityMessageResolver;

    protected AbstractAuthenticationHandler(ObjectMapper objectMapper, SecurityMessageResolver securityMessageResolver) {
        Assert.notNull(objectMapper, "JSON 映射器不能为空。");
        Assert.notNull(securityMessageResolver, "Security 消息解析器不能为空。");
        this.objectMapper = objectMapper;
        this.securityMessageResolver = securityMessageResolver;
    }

    /**
     * 写出认证失败响应。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param exception 认证异常
     * @throws IOException
     */
    /**
     * write Failed
     *
     * @param request 参数 request
     * @param response 参数 response
     * @param exception 参数 exception
     */
    protected void writeFailed(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        String messageCode = exception.getMessage();
        Locale locale = LocaleContextHolder.getLocale();
        String resolvedMessage = securityMessageResolver.resolve(messageCode, null, locale, messageCode);

        if (log.isDebugEnabled()) {
            log.debug("认证失败：code={}, message={}", messageCode, resolvedMessage);
        }
        ResultModel<Object> failed = ResultModel.failed(response.getStatus(), exception, resolvedMessage, request.getRequestURI());
        writeJson(response, failed);
    }

    /**
     * 写出认证成功响应。
     *
     * @param response 当前响应
     * @param body 响应体
     * @throws IOException
     */
    /**
     * write Success
     *
     * @param response 参数 response
     * @param body 参数 body
     */
    protected void writeSuccess(HttpServletResponse response, Object body) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        writeJson(response, body);
    }

    /**
     * 将对象按 JSON 格式写入响应。
     *
     * @param response 当前响应
     * @param body 响应体
     */
    private void writeJson(HttpServletResponse response, Object body) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

}
