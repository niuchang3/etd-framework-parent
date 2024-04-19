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
    public void ignore() {
    }

    @Around("ignore()")
    public Object generateDataSnapshot(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            RequestContext.setIgnoreTenant(true);
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw e;
        } finally {
            RequestContext.setIgnoreTenant(false);
        }
    }
}
