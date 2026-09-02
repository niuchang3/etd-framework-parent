package org.etd.framework.starter.mybaits.permission.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.mybaits.permission.annotation.DataPermission;
import org.etd.framework.starter.mybaits.permission.annotation.IgnoreDataPermission;
import org.etd.framework.starter.mybaits.permission.context.DataPermissionContextHolder;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

/**
 * 数据权限切面类
 * <p>
 * 处理 @DataPermission 与 @IgnoreDataPermission 注解的切面拦截与上下文状态切换。
 *
 * @author 牛昌
 */
@Aspect
public class DataPermissionAspect {

    @Pointcut("@annotation(org.etd.framework.starter.mybaits.permission.annotation.IgnoreDataPermission) " +
              "|| @within(org.etd.framework.starter.mybaits.permission.annotation.IgnoreDataPermission)")
    /**
     * ignore Data Permission Pointcut
     *
     */
    public void ignoreDataPermissionPointcut() {
    }

    @Pointcut("@annotation(org.etd.framework.starter.mybaits.permission.annotation.DataPermission) " +
              "|| @within(org.etd.framework.starter.mybaits.permission.annotation.DataPermission)")
    /**
     * data Permission Pointcut
     *
     */
    public void dataPermissionPointcut() {
    }

    @Around("ignoreDataPermissionPointcut()")
    /**
     * 处理 Ignore Data Permission
     *
     * @param joinPoint 参数 joinPoint
     * @return 处理结果
     */
    public Object handleIgnoreDataPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean previousState = RequestContext.getIgnoreDataPermission();
        try {
            RequestContext.setIgnoreDataPermission(true);
            return joinPoint.proceed();
        } finally {
            RequestContext.setIgnoreDataPermission(previousState);
        }
    }

    @Around("dataPermissionPointcut()")
    /**
     * 处理 Data Permission
     *
     * @param joinPoint 参数 joinPoint
     * @return 处理结果
     */
    public Object handleDataPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        DataPermission annotation = findAnnotation(joinPoint);
        if (annotation != null) {
            DataPermissionContextHolder.push(annotation);
        }
        try {
            return joinPoint.proceed();
        } finally {
            if (annotation != null) {
                DataPermissionContextHolder.pop();
            }
        }
    }

    /**
     * 解析方法或类上的 @DataPermission 注解（优先使用方法上的注解）
     */
    private DataPermission findAnnotation(ProceedingJoinPoint joinPoint) {
        if (joinPoint.getSignature() instanceof MethodSignature methodSignature) {
            Method method = methodSignature.getMethod();
            Class<?> targetClass = joinPoint.getTarget().getClass();
            Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
            DataPermission methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(
                    targetMethod, DataPermission.class);
            if (methodAnnotation != null) {
                return methodAnnotation;
            }
            return AnnotatedElementUtils.findMergedAnnotation(targetClass, DataPermission.class);
        }
        return null;
    }
}
