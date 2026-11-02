package org.etd.framework.starter.ai.runtime.autoconfigure;

import org.etd.framework.starter.ai.agent.AgentExecutor;
import org.etd.framework.starter.ai.agent.ReactAgentExecutor;
import org.etd.framework.starter.ai.memory.AgentMemoryManager;
import org.etd.framework.starter.ai.model.autoconfigure.ModelClientAutoConfiguration;
import org.etd.framework.starter.ai.persistence.spi.AgentRunStore;
import org.etd.framework.starter.ai.persistence.spi.AgentRunLeaseStore;
import org.etd.framework.starter.ai.runtime.AgentChatService;
import org.etd.framework.starter.ai.runtime.AgentTaskRunner;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.etd.framework.starter.ai.tooling.AgentToolAuthorization;
import org.etd.framework.starter.ai.runtime.spi.AgentChatPrepareService;
import org.etd.framework.starter.ai.memory.semantic.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * 智能体运行时自动配置：装配记忆管理、执行中枢、任务守护底座与聊天服务门面。
 */
@AutoConfiguration(after = ModelClientAutoConfiguration.class)
@EnableConfigurationProperties(AgentRuntimeProperties.class)
public class AgentRuntimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AgentMemoryManager agentMemoryManager() {
        return new AgentMemoryManager();
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentExecutor agentExecutor(AgentMemoryManager memoryManager, ObjectProvider<AgentToolAuthorization> authorization,
            ObjectProvider<ModelClientRegistry> models, ObjectProvider<AgentKnowledgeQueryService> knowledge,
            ObjectProvider<ToolCallbackProvider> tools, AgentRuntimeProperties properties) {
        AgentToolAuthorization policy = authorization.getIfAvailable();
        return new ReactAgentExecutor(memoryManager, policy, models, knowledge, tools, properties);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public AgentTaskRunner agentTaskRunner(AgentRuntimeProperties properties,
            ObjectProvider<AgentRunLeaseStore> runStores) {
        return new AgentTaskRunner(properties, runStores);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentChatService agentChatService(AgentTaskRunner taskRunner, AgentExecutor executor,
            AgentRuntimeProperties properties, ObjectProvider<AgentChatPrepareService> preparation,
            ObjectProvider<AgentRunStore> records, ObjectProvider<AgentRunLeaseStore> leases) {
        return new AgentChatService(taskRunner, executor, properties, preparation, records, leases);
    }
}
