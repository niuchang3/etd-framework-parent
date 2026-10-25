package org.etd.framework.starter.ai.model.autoconfigure;

import io.micrometer.observation.ObservationRegistry;
import java.util.List;
import org.etd.framework.starter.ai.model.client.ModelClientFactory;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.model.client.ModelClientTransport;
import org.etd.framework.starter.ai.model.config.ModelClientProperties;
import org.etd.framework.starter.ai.model.provider.DashScopeModelClientProvider;
import org.etd.framework.starter.ai.model.provider.OpenAiModelClientProvider;
import org.etd.framework.starter.ai.model.provider.OllamaModelClientProvider;
import org.etd.framework.starter.ai.model.spi.ModelClientProvider;
import org.etd.framework.starter.ai.model.spi.ModelConnectionResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** 仅注册单例工厂及管理器，不创建默认模型，不要求全局API Key。 */
@AutoConfiguration
@EnableConfigurationProperties(ModelClientProperties.class)
public class ModelClientAutoConfiguration {
    /** @param properties HTTP配置 @return 无认证状态的共享传输 */
    @Bean
    @ConditionalOnMissingBean
    public ModelClientTransport modelClientTransport(ModelClientProperties properties) {
        return new ModelClientTransport(properties);
    }

    /** @param transport 共享传输 @param observations 可选观测 @return DashScope适配器 */
    @Bean
    @ConditionalOnMissingBean
    public DashScopeModelClientProvider dashScopeModelClientProvider(ModelClientTransport transport,
            ObjectProvider<ObservationRegistry> observations) {
        var registry = observations.getIfAvailable(() -> ObservationRegistry.NOOP);
        return new DashScopeModelClientProvider(transport, registry);
    }

    /** @param transport 共享传输 @param observations 可选观测 @return OpenAI兼容适配器 */
    @Bean
    @ConditionalOnMissingBean
    public OpenAiModelClientProvider openAiModelClientProvider(ModelClientTransport transport,
            ObjectProvider<ObservationRegistry> observations) {
        var registry = observations.getIfAvailable(() -> ObservationRegistry.NOOP);
        return new OpenAiModelClientProvider(transport, registry);
    }

    /** @param transport 共享传输 @param observations 可选观测 @return Ollama适配器 */
    @Bean
    @ConditionalOnMissingBean
    public OllamaModelClientProvider ollamaModelClientProvider(ModelClientTransport transport,
            ObjectProvider<ObservationRegistry> observations) {
        var registry = observations.getIfAvailable(() -> ObservationRegistry.NOOP);
        return new OllamaModelClientProvider(transport, registry);
    }

    /** @param providers 已注册适配器 @return 无租户状态的统一工厂 */
    @Bean
    @ConditionalOnMissingBean
    public ModelClientFactory modelClientFactory(List<ModelClientProvider> providers) {
        return new ModelClientFactory(providers);
    }

    /** @param factory 统一工厂 @param resolver 业务提供的授权解析器 @param properties 缓存配置 @return 节点内实例管理器 */
    @Bean
    @ConditionalOnBean(ModelConnectionResolver.class)
    @ConditionalOnMissingBean
    public ModelClientRegistry modelClientRegistry(ModelClientFactory factory, ModelConnectionResolver resolver,
            ModelClientProperties properties) {
        return new ModelClientRegistry(factory, resolver, properties);
    }
}
