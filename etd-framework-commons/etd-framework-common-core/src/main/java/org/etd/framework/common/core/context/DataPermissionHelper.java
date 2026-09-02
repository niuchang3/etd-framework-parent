package org.etd.framework.common.core.context;

import org.etd.framework.common.core.context.model.RequestContext;

/**
 * 数据权限编排控制辅助工具类
 * <p>
 * 提供 try-with-resources 作用域支持，实现局部临时忽略或恢复数据权限拦截。
 *
 * @author 牛昌
 */
public class DataPermissionHelper {

    private DataPermissionHelper() {
    }

    /**
     * 开启忽略数据权限过滤的作用域（try-with-resources 方式）
     *
     * @return Scope 自动恢复作用域对象
     */
    public static Scope ignore() {
        return new Scope(true);
    }

    /**
     * 显式指定数据权限忽略标志的作用域
     *
     * @param ignore 是否忽略数据权限
     * @return Scope 自动恢复作用域对象
     */
    public static Scope use(boolean ignore) {
        return new Scope(ignore);
    }

    /**
     * 数据权限控制标志恢复作用域
     */
    public static class Scope implements AutoCloseable {
        private final boolean previousFlag;

        public Scope(boolean ignore) {
            this.previousFlag = RequestContext.getIgnoreDataPermission();
            RequestContext.setIgnoreDataPermission(ignore);
        }

        @Override
        public void close() {
            RequestContext.setIgnoreDataPermission(previousFlag);
        }
    }
}
