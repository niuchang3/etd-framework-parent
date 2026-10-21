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
        String[] resources = resolveResources(classPermission, methodPermission);
        PermissionAction action = resolveAction(classPermission, methodPermission, request.getMethod());
        if (action == PermissionAction.WRITE && isReadOnly(root)) {
            return false;
        }
        return isPlatformAdmin(root) || hasAnyAuthority(root, resources, action);
    }

    /** 各资源共享本次操作类型，命中任意一个权限即可访问。 */
    private boolean hasAnyAuthority(MethodSecurityExpressionOperations root, String[] resources,
                                    PermissionAction action) {
        for (String resource : resources) {
            String authority = PermissionCode.createAuthority(resource, action);
            if (root.hasAuthority(authority)) {
                return true;
            }
        }
        return false;
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

    /** 方法非空数组整体覆盖类声明；最终必须至少有一个合法资源码。 */
    private String[] resolveResources(Permission classPermission, Permission methodPermission) {
        validateResources(classPermission);
        validateResources(methodPermission);
        String[] resources = classPermission == null ? new String[0] : classPermission.value();
        if (hasResourceCode(methodPermission)) {
            resources = methodPermission.value();
        }
        if (resources.length == 0) {
            throw new IllegalArgumentException("权限注解未提供合法资源码");
        }
        return resources;
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
        return permission != null && permission.value().length > 0;
    }

    /** 注解是否显式指定读写类型，而非 AUTO。 */
    private boolean hasExplicitAction(Permission permission) {
        return permission != null && permission.action() != PermissionAction.AUTO;
    }

    /** 空数组表示继承；提前校验全部元素，避免短路放行掩盖错误配置。 */
    private void validateResources(Permission permission) {
        if (permission == null) {
            return;
        }
        for (String resource : permission.value()) {
            if (!PermissionCode.isValidResource(resource)) {
                throw new IllegalArgumentException("权限资源码格式错误：" + resource);
            }
        }
    }
}
