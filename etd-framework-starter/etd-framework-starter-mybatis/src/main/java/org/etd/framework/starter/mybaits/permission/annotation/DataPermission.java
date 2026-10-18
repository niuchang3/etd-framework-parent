package org.etd.framework.starter.mybaits.permission.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限控制注解
 * <p>
 * 可标注在 Mapper 方法、Service 方法或 Controller 方法上，用于精细化配置数据权限规则。
 *
 * @author 牛昌
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 是否启用数据权限过滤，默认开启
     *
     * @return 是否启用
     */
    boolean enable() default true;

    /**
     * 按真实表名声明参与过滤的表；非空时只过滤匹配表，别名默认从SQL自动取得。
     * 未声明时沿用原有全表或alias范围；各表条件的连接位置由SQL拦截器决定。
     *
     * @return 表级权限规则
     */
    DataPermissionTable[] tables() default {};

    /**
     * 主表别名，如 SQL 中的 "t." 或 "user."
     *
     * @return 表别名
     */
    String alias() default "";

    /**
     * 部门/组织字段名称，默认为空时使用全局配置（如 org_id）
     *
     * @return 部门字段名称
     */
    String orgColumn() default "";

    /**
     * 用户/创建人字段名称，默认为空时使用全局配置（如 create_by）
     *
     * @return 用户字段名称
     */
    String userColumn() default "";
}
