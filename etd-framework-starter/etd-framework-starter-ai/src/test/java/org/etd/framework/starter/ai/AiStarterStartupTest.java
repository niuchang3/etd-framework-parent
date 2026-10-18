package org.etd.framework.starter.ai;

import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证依赖入口不会在无全局模型凭证时触发供应商客户端的强制初始化。 */
class AiStarterStartupTest {

    /** 加载 classpath 自动配置，但不提供 API Key 或模型关闭开关。 */
    @Test
    void shouldStartWithoutGlobalModelCredentials() {
        ApplicationContextRunner runner = new ApplicationContextRunner();
        runner.withUserConfiguration(StartupConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ChatModel.class);
            assertThat(context).doesNotHaveBean(EmbeddingModel.class);
            assertThat(context).doesNotHaveBean(DashScopeAgent.class);
        });
    }

    /** 使用真实自动配置发现机制，防止依赖变更重新引入全局供应商客户端。 */
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class StartupConfiguration {
    }
}
