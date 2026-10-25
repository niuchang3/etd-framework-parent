package org.etd.framework.starter.ai.model.client;

import java.util.List;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.spi.ModelClientProvider;
import org.springframework.util.Assert;
import org.springframework.ai.model.Model;

/** 无租户状态的实例工厂；动态连接由Registry授权并缓存，不注册成大量Spring Bean。 */
public class ModelClientFactory {
    private final List<ModelClientProvider> providers;

    /** @param providers 协议适配器，同一协议及能力必须唯一匹配 */
    public ModelClientFactory(List<ModelClientProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    /** @param configuration 已授权配置 @return 原生SDK模型；共享传输独立管理，不发送探测请求 */
    public Model<?, ?> create(ModelConnectionConfiguration configuration) {
        Assert.isTrue(configuration.enabled(), "模型连接已停用");
        ModelClientProvider selected = selectProvider(configuration);
        try {
            var instance = selected.create(configuration);
            Assert.notNull(instance, "模型适配器未返回实例");
            Assert.isTrue(configuration.capability().getModelType().isInstance(instance), "模型实例与配置能力不匹配");
            return instance;
        } catch (RuntimeException exception) {
            // SDK初始化异常可能带入地址和凭证，不将原始异常作为cause向外传播。
            throw new IllegalStateException("模型客户端初始化失败，请检查连接配置");
        }
    }

    private ModelClientProvider selectProvider(ModelConnectionConfiguration configuration) {
        ModelClientProvider selected = null;
        for (var provider : providers) {
            if (provider.supports(configuration.protocol(), configuration.capability())) {
                Assert.state(selected == null, "模型协议与能力匹配了多个适配器");
                selected = provider;
            }
        }
        Assert.state(selected != null, "当前没有支持该协议与模型能力的适配器");
        return selected;
    }
}
