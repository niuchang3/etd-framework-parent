package com.etd.framework.starter.client.core.permission.annotation;

import org.etd.framework.common.core.constants.PermissionAction;
import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

/** 声明 MVC 接口的菜单资源权限；方法逐属性覆盖类声明，由原生方法授权执行校验。 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("@permissionChecker.check(#root)")
public @interface Permission {
    /** 完整资源码；方法留空时继承类上的资源码。 */
    String value() default "";

    /** 方法 AUTO 时继承类上的显式操作，仍为 AUTO 才按 HTTP 方法推导。 */
    PermissionAction action() default PermissionAction.AUTO;
}
