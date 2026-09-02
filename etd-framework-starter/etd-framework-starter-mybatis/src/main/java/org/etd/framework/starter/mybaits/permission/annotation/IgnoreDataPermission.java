package org.etd.framework.starter.mybaits.permission.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 忽略数据权限过滤注解
 * <p>
 * 标注在类或方法上时，当前方法执行过程中的 SQL 查询与修改操作将跳过数据权限 SQL 追加。
 *
 * @author 牛昌
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreDataPermission {
}
