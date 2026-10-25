package org.etd.framework.starter.ai.model.config;

import java.net.URI;
import java.util.function.Supplier;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.springframework.util.Assert;

/**
 * 已授权连接的不可变配置快照，不保存数据库实体或会话状态。
 * @param tenantId 连接所属租户，必须由可信服务确定
 * @param connectionId 连接标识
 * @param version 单调递增配置版本；地址、凭证、模型及能力变化均必须递增
 * @param enabled 连接及其模型是否均启用
 * @param protocol 显式接入协议代码
 * @param capability 此连接的模型能力
 * @param model 模型服务使用的调用代码
 * @param baseUrl 包含实际API前缀的根地址，不接受查询参数、片段或URI内凭证
 * @param apiKey 仅在初始化缓存实例时调用的凭证供应器；无认证时返回null
 */
public record ModelConnectionConfiguration(String tenantId, String connectionId, long version, boolean enabled,
        String protocol, ModelCapability capability, String model, URI baseUrl, Supplier<String> apiKey) {
    public ModelConnectionConfiguration {
        Assert.hasText(tenantId, "租户标识不能为空");
        Assert.hasText(connectionId, "连接标识不能为空");
        Assert.isTrue(version >= 0, "模型配置版本不能为负");
        Assert.hasText(protocol, "模型协议不能为空");
        Assert.notNull(capability, "模型能力不能为空");
        Assert.hasText(model, "模型调用代码不能为空");
        Assert.notNull(baseUrl, "模型根地址不能为空");
        Assert.isTrue(("http".equals(baseUrl.getScheme()) || "https".equals(baseUrl.getScheme()))
                && baseUrl.getHost() != null && baseUrl.getUserInfo() == null
                && baseUrl.getQuery() == null && baseUrl.getFragment() == null, "模型根地址格式不正确");
        Assert.notNull(apiKey, "凭证供应器不能为空，无认证时应返回null");
    }

    /** @return 当前凭证，仅在实例初始化时调用；null明确表示无认证，空白凭证拒绝使用 */
    public String resolveApiKey() {
        String secret = apiKey.get();
        Assert.isTrue(secret == null || !secret.isBlank(), "模型凭证不能是空白字符串");
        return secret;
    }

    /** 避免日志或调试输出意外包含地址、模型内容或凭证供应器。 */
    @Override
    public String toString() {
        return "ModelConnectionConfiguration[redacted]";
    }
}
