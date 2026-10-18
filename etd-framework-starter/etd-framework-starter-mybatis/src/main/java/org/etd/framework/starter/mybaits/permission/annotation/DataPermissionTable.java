package org.etd.framework.starter.mybaits.permission.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 表级数据权限规则；公开字段只扩大已登录用户的读取范围，不扩大写入范围。 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermissionTable {
    /** @return 真实表名，可包含schema；必须为后端固定标识符，不接受SQL表达式 */
    String table();

    /** @return 可选表别名；为空时匹配该表的所有引用，并自动使用各自的SQL别名 */
    String alias() default "";

    /** @return 布尔类型公开字段；为空时不启用公开读取，数据库必须存在所声明字段 */
    String publicColumn() default "";

    /** @return 组织字段；为空时采用外层注解或框架默认配置 */
    String orgColumn() default "";

    /** @return 创建人字段；为空时采用外层注解或框架默认配置 */
    String userColumn() default "";
}
