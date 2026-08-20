package com.etd.framework.starter.oauth.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.model.ResultModel;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

@Slf4j
public abstract class AbstractAuthenticationHandler {
    private final ObjectMapper objectMapper;

    public AbstractAuthenticationHandler() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(simpleModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    /**
     * 异常信息返回
     *
     * @param request
     * @param response
     * @param exception
     * @throws IOException
     */
    protected void writeFailed(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {
        if(log.isDebugEnabled()){
            log.debug("认证失败：{}", exception.getMessage());
        }

        ResultModel<Object> failed = ResultModel.failed(response.getStatus(), exception.getCause(), exception.getMessage(), request.getRequestURI());
        writeJson(response, failed);
    }

    /**
     * 成功信息返回
     *
     * @param response
     * @param body
     * @throws IOException
     */
    protected void writeSuccess(HttpServletResponse response, ResultModel<?> body) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        writeJson(response, body);

    }

    private void writeJson(HttpServletResponse response, Object body) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

}
