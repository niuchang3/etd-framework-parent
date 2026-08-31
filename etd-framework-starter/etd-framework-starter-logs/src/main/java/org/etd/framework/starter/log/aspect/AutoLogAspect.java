package org.etd.framework.starter.log.aspect;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.etd.framework.starter.log.constant.LogConstant;
import org.etd.framework.starter.log.dto.LogInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 自动日志记录切面
 *
 * @author Young
 * @date 2020/12/14
 */
@Slf4j
@Aspect
@Component
@ConditionalOnClass({HttpServletRequest.class, RequestContextHolder.class})
public class AutoLogAspect {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Around("@annotation(autoLog) || @within(autoLog)")
    public Object around(ProceedingJoinPoint joinPoint, AutoLog autoLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        LogInfo logInfo = LogInfo.getInstance(joinPoint, autoLog);
        try {
            Object proceed = joinPoint.proceed();
            logInfo.setCostTime(System.currentTimeMillis() - startTime);
            log.info("{}", toJson(logInfo));
            return proceed;
        } catch (Throwable throwable) {
            logInfo.setCostTime(System.currentTimeMillis() - startTime);
            logInfo.setLogType(LogConstant.LOG_TYPE.ERROR.getCode());
            logInfo.setMessage(ExceptionUtil.stacktraceToString(throwable));
            log.error("{}", toJson(logInfo));
            throw throwable;
        }
    }

    private String toJson(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.warn("AutoLogAspect JSON 序列化失败: {}", e.getMessage());
            return String.valueOf(object);
        }
    }
}
