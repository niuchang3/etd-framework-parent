package org.etd.framework.starter.ai.model.spi;

import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.springframework.ai.model.Model;

/** 协议适配扩展点；注册为Bean即可接入，禁止在创建客户端时进行模型调用或下载。 */
public interface ModelClientProvider {
    /** @param protocol 协议代码 @param capability 模型能力 @return 是否支持该组合 */
    boolean supports(String protocol, ModelCapability capability);
    /** @param configuration 已授权且启用的不可变配置 @return 原生SDK模型，须支持并发调用；不得持有需要逐实例关闭的独占资源 */
    Model<?, ?> create(ModelConnectionConfiguration configuration);
}
