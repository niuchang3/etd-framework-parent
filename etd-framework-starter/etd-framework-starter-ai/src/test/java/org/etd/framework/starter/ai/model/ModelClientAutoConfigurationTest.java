package org.etd.framework.starter.ai.model;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import org.etd.framework.starter.ai.model.autoconfigure.ModelClientAutoConfiguration;
import org.etd.framework.starter.ai.model.client.ModelClientFactory;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.model.config.ModelClientProperties;
import org.etd.framework.starter.ai.model.config.ModelConnectionConfiguration;
import org.etd.framework.starter.ai.model.constant.ModelCapability;
import org.etd.framework.starter.ai.model.constant.ModelClientProtocol;
import org.etd.framework.starter.ai.model.spi.ModelConnectionResolver;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.*;

/** 自动配置无需业务表或全局密钥；只有业务提供授权解析器时才注册实例管理器。 */
class ModelClientAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ModelClientAutoConfiguration.class));

    /** 缺少Resolver时只提供工厂，不创建默认模型或可绕过授权的实例缓存。 */
    @Test
    void shouldStartWithoutResolverAndCredentials() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(ModelClientFactory.class);
            assertThat(context).doesNotHaveBean(ModelClientRegistry.class);
            assertThat(context).doesNotHaveBean(ChatModel.class).doesNotHaveBean(EmbeddingModel.class);
        });
    }

    /** 接入Resolver后仍按需初始化，配置绑定可覆盖安全默认值。 */
    @Test
    void shouldConfigureRegistryWithoutResolvingConnectionsAtStartup() {
        ModelConnectionResolver resolver = (tenant, id) -> { throw new AssertionError("启动不得解析租户连接"); };
        runner.withBean(ModelConnectionResolver.class, () -> resolver)
                .withPropertyValues("etd.ai.model.clients.maximum-size=16", "etd.ai.model.clients.expire-after-access=2m")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(ModelClientRegistry.class);
                    assertThat(context.getBean(ModelClientProperties.class).getMaximumSize()).isEqualTo(16);
                });
    }

    /** 获取模型时才创建SDK，管理器本身保持Spring单例。 */
    @Test
    void shouldReuseLazyClientsAfterResolverIsProvided() {
        URI uri = URI.create("https://example.invalid/v1");
        AtomicInteger decrypted = new AtomicInteger();
        ModelConnectionConfiguration configuration = new ModelConnectionConfiguration("tenant", "1", 0, true, ModelClientProtocol.OPENAI_COMPATIBLE,
                ModelCapability.CHAT, "test-model", uri, () -> { decrypted.incrementAndGet(); return null; });
        ModelConnectionResolver resolver = (tenant, id) -> configuration;
        runner.withBean(ModelConnectionResolver.class, () -> resolver).run(context -> {
            ModelClientRegistry registry = context.getBean(ModelClientRegistry.class);
            assertThat(context.getBean(ModelClientRegistry.class)).isSameAs(registry);
            ChatModel first = registry.getChatModel("tenant", "1");
            ChatModel second = registry.getChatModel("tenant", "1");
            assertThat(first).isSameAs(second);
            assertThat(decrypted).hasValue(1);
        });
    }
}
