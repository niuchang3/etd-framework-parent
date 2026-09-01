package org.etd.framework.starter.mybaits.tenant.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.etd.framework.common.core.context.model.RequestContext;

@Slf4j
@Aspect
public class IgnoreTenantAspect {

    @Pointcut("@annotation(org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant)")
    public void ignoreTenant() {
    }

    @Around("ignoreTenant()")
    public Object ignoreTenant(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean previousIgnoreTenant = RequestContext.getIgnoreTenant();
        try {
            RequestContext.setIgnoreTenant(true);
            return joinPoint.proceed();
        } finally {
            // 支持嵌套调用，恢复进入切面前的状态而不是固定关闭。
            RequestContext.setIgnoreTenant(previousIgnoreTenant);
        }
    }
}
