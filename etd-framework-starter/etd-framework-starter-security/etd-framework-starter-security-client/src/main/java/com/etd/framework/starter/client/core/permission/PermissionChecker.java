package com.etd.framework.starter.client.core.permission;

import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.servlet.http.HttpServletRequest;
import org.etd.framework.common.core.constants.PermissionAction;
import org.etd.framework.common.core.constants.PermissionCode;
import org.etd.framework.common.core.user.UserDetails;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/** 解析当前 MVC 接口的权限注解，权限匹配直接使用 Spring Security 表达式根对象。 */
public final class PermissionChecker {

    /** 合并类和方法声明；平台管理员放行，其余用户按实际权限码校验。 */
    public boolean check(MethodSecurityExpressionOperations root) {
        HttpServletRequest request = resolveCurrentRequest();
        if (request == null) {
            return false;
        }
        HandlerMethod handler = resolveCurrentHandler(request, root.getThis());
        if (handler == null) {
            return false;
        }
        Permission classPermission = AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), Permission.class);
        Permission methodPermission = handler.getMethodAnnotation(Permission.class);
        if (classPermission == null && methodPermission == null) {
            return false;
        }
        String resource = resolveResource(classPermission, methodPermission);
        PermissionAction action = resolveAction(classPermission, methodPermission, request.getMethod());
        if (action == PermissionAction.WRITE && isReadOnly(root)) {
            return false;
        }
        String authority = PermissionCode.createAuthority(resource, action);
        return isPlatformAdmin(root) || root.hasAuthority(authority);
    }

    /** 获取当前 HTTP 请求；非 MVC 请求上下文不适用接口权限注解。 */
    private HttpServletRequest resolveCurrentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /** 仅接受当前目标 Controller 的映射，防止其他 Bean 借用它的权限声明。 */
    private HandlerMethod resolveCurrentHandler(HttpServletRequest request, Object target) {
        Object matchedHandler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (!(matchedHandler instanceof HandlerMethod handler)) {
            return null;
        }
        if (!handler.getBeanType().isInstance(target)) {
            return null;
        }
        return handler;
    }

    /** 只有平台管理员享有接口权限豁免，租户管理员仍按权限码校验。 */
    private boolean isPlatformAdmin(MethodSecurityExpressionOperations root) {
        return root.getAuthentication().getDetails() instanceof UserDetails user && user.isPlatformAdmin();
    }

    /** 锁定用户或锁定租户统一回收写权限，平台管理员也不能绕过该安全状态。 */
    private boolean isReadOnly(MethodSecurityExpressionOperations root) {
        return root.getAuthentication().getDetails() instanceof UserDetails user && user.isReadOnly();
    }

    /** 方法填写资源码时覆盖类声明，否则继承类上的资源码。 */
    private String resolveResource(Permission classPermission, Permission methodPermission) {
        validateResource(classPermission);
        validateResource(methodPermission);
        String resource = classPermission == null ? "" : classPermission.value();
        if (hasResourceCode(methodPermission)) {
            resource = methodPermission.value();
        }
        if (!PermissionCode.isValidResource(resource)) {
            throw new IllegalArgumentException("权限注解未提供合法资源码");
        }
        return resource;
    }

    /** 显式声明优先；类和方法都使用 AUTO 时，才按 HTTP 方法判断读写。 */
    private PermissionAction resolveAction(Permission classPermission, Permission methodPermission, String httpMethod) {
        if (hasExplicitAction(methodPermission)) {
            return methodPermission.action();
        }
        if (hasExplicitAction(classPermission)) {
            return classPermission.action();
        }
        return switch (httpMethod) {
            case "GET", "HEAD" -> PermissionAction.READ;
            case "POST", "PUT", "PATCH", "DELETE" -> PermissionAction.WRITE;
            default -> throw new IllegalArgumentException("无法自动确定读写类型，请显式声明 action：" + httpMethod);
        };
    }

    /** 注解是否填写了资源码；未填写时继承类声明。 */
    private boolean hasResourceCode(Permission permission) {
        return permission != null && !permission.value().isEmpty();
    }

    /** 注解是否显式指定读写类型，而非 AUTO。 */
    private boolean hasExplicitAction(Permission permission) {
        return permission != null && permission.action() != PermissionAction.AUTO;
    }

    /** 留空表示继承，非空声明必须遵守菜单资源码格式。 */
    private void validateResource(Permission permission) {
        if (hasResourceCode(permission) && !PermissionCode.isValidResource(permission.value())) {
            throw new IllegalArgumentException("权限资源码格式错误：" + permission.value());
        }
    }
}
