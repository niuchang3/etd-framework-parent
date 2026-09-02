package org.etd.framework.starter.mybaits.tenant.annotation;


import java.lang.annotation.*;

/**
 * 忽略多租户行级过滤注解。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreTenant {
}
