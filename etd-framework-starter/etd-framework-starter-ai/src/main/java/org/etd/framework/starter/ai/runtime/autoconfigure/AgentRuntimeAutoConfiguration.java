package org.etd.framework.starter.ai.runtime.autoconfigure;

import org.etd.framework.starter.ai.runtime.AgentExecutionEngine;
import org.etd.framework.starter.ai.runtime.AgentRuntime;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.spi.AgentRunRecordService;
import org.springframework.beans.factory.ObjectProvider;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/** 注册无业务状态的运行引擎，不创建默认模型或业务会话。 */
@AutoConfiguration
@EnableConfigurationProperties(AgentRuntimeProperties.class)
public class AgentRuntimeAutoConfiguration {
    /** @param engine 执行引擎 @param properties 运行策略 @param recordServices 业务生命周期插槽 @return 不绑定HTTP及数据库的运行容器 */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public AgentRuntime agentRuntime(AgentExecutionEngine engine, AgentRuntimeProperties properties,
            ObjectProvider<AgentRunRecordService> recordServices) {
        Supplier<List<AgentRunRecordService>> providers = () -> recordServices.orderedStream().toList();
        return new AgentRuntime(engine, properties, providers);
    }

    /** @return 可由应用替换的通用Agent执行器 */
    @Bean
    @ConditionalOnMissingBean
    public AgentExecutionEngine agentExecutionEngine() {
        return new AgentExecutionEngine();
    }
}
