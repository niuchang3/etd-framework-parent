package org.etd.framework.starter.ai.model.spi;

import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;

/** 业务实现的连接解析边界；框架不依赖业务表、登录上下文或凭证存储方式。 */
@FunctionalInterface
public interface ModelConnectionResolver {
    /**
     * 每次获取租约都会调用，必须重新校验租户访问、删除及启停状态，不得仅凭前端租户标识放行。
     * @param tenantId 预期归属租户
     * @param connectionId 预期连接
     * @return 当前最新已授权配置，凭证可延迟解密；配置不得来自滞后的本地缓存
     * @throws RuntimeException 无权限、连接不存在或无法读取最新配置时拒绝获取
     */
    ModelConnectionConfiguration resolveAuthorized(String tenantId, String connectionId);
}
