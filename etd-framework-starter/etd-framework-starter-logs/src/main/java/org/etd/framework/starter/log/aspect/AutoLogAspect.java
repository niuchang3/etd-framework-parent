package org.etd.framework.starter.log.aspect;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.google.gson.Gson;
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

import javax.servlet.http.HttpServletRequest;

/**
 * @author Young
 * @description
 * @date 2020/12/14
 */

@Slf4j
@Aspect
@Component
@ConditionalOnClass({HttpServletRequest.class, RequestContextHolder.class})
public class AutoLogAspect {


    @Around("@within(autoLog) || @annotation(autoLog)")
    public Object beforeMethod(ProceedingJoinPoint joinPoint, AutoLog autoLog) throws Throwable {
        LogInfo logInfo = LogInfo.getInstance(joinPoint, autoLog);
        try {
            Object proceed = joinPoint.proceed();

            log.info("{}", new Gson().toJson(logInfo));
            return proceed;
        } catch (Throwable throwable) {
            logInfo.setLogType(LogConstant.LOG_TYPE.error.name());
            logInfo.setMessage(ExceptionUtil.stacktraceToString(throwable));
            log.error("{}", new Gson().toJson(logInfo));
            throw throwable;
        }
    }

}
