package org.etd.framework.starter.mybaits.tenant.aspect;

import com.etd.framework.starter.client.core.user.UserDetails;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.etd.framework.common.core.context.model.RequestContext;

@Slf4j
@Aspect
public class IgnoreTenantAspect {

    public IgnoreTenantAspect() {
        System.out.println("2");
    }

    @Pointcut("@annotation(org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant)")
    public void ignoreTenant() {
    }

    @Around("ignoreTenant()")
    public Object ignoreTenant(ProceedingJoinPoint joinPoint) throws Throwable {
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
