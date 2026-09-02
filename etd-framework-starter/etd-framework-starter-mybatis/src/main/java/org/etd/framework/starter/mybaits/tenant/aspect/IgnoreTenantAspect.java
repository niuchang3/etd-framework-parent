package org.etd.framework.starter.mybaits.tenant.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.etd.framework.common.core.context.model.RequestContext;

/**
 * 多租户过滤忽略切面拦截器。
 * <p>
 * 处理带有 {@link org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant} 注解的方法，
 * 在方法执行期间临时关闭多租户 SQL 行级过滤。
 */
@Slf4j
@Aspect
public class IgnoreTenantAspect {

    @Pointcut("@annotation(org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant)")
    /**
     * ignore Tenant
     *
     */
    public void ignoreTenant() {
    }

    @Around("ignoreTenant()")
    /**
     * ignore Tenant
     *
     * @param joinPoint 参数 joinPoint
     * @return 处理结果
     */
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
