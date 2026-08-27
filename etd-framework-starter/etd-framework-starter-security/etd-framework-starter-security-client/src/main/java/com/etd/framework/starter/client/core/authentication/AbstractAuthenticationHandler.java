package com.etd.framework.starter.client.core.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证响应处理器基类。
 * <p>
 * 统一处理 JSON 序列化和响应状态码，避免各个安全处理器重复写响应。
 */
@Slf4j
public abstract class AbstractAuthenticationHandler {
    private final ObjectMapper objectMapper;

    protected AbstractAuthenticationHandler(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "JSON 映射器不能为空。");
        this.objectMapper = objectMapper;
    }


    /**
     * 写出认证失败响应。
     *
     * @param request 当前请求
     * @param response 当前响应
     * @param exception 认证异常
     * @throws IOException
     */
    protected void writeFailed(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("认证失败：{}", exception.getMessage());
        }

        Map<String, Object> failed = responseBody(response.getStatus(), null, exception.getMessage(), null, request.getRequestURI());
        writeJson(response, failed);
    }

    /**
     * 写出认证成功响应。
     *
     * @param response 当前响应
     * @param body 响应体
     * @throws IOException
     */
    protected void writeSuccess(HttpServletResponse response, Object body) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        writeJson(response, body);

    }

    /**
     * 构建统一响应体。
     *
     * @param code 响应码
     * @param devMessage 开发提示
     * @param message 用户提示
     * @param data 响应数据
     * @param url 请求地址
     * @return 统一响应体
     */
    protected Map<String, Object> responseBody(Integer code, String devMessage, String message, Object data, String url) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("devMessage", devMessage);
        body.put("message", message);
        body.put("data", data);
        body.put("url", url);
        return body;
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
