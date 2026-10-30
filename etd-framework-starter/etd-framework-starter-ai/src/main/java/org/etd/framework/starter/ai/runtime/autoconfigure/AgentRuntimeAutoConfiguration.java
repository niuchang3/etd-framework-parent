package org.etd.framework.starter.ai.runtime.autoconfigure;

import org.etd.framework.starter.ai.runtime.AgentExecutionEngine;
import org.etd.framework.starter.ai.runtime.AgentRuntime;
import org.etd.framework.starter.ai.runtime.AgentChatService;
import org.etd.framework.starter.ai.model.autoconfigure.ModelClientAutoConfiguration;
import org.etd.framework.starter.ai.model.client.ModelClientRegistry;
import org.etd.framework.starter.ai.runtime.spi.AgentChatPrepareService;
import org.etd.framework.starter.ai.runtime.spi.AgentKnowledgeQueryService;
import org.etd.framework.starter.ai.runtime.config.AgentRuntimeProperties;
import org.etd.framework.starter.ai.runtime.spi.AgentRunRecordService;
import org.springframework.beans.factory.ObjectProvider;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/** 注册无业务状态的运行引擎，不创建默认模型或业务会话。 */
@AutoConfiguration(after = ModelClientAutoConfiguration.class)
@EnableConfigurationProperties(AgentRuntimeProperties.class)
public class AgentRuntimeAutoConfiguration {
    /** 仅在业务实现三个接口及模型解析器后注册统一入口；业务无需手工连接各接口。
     * @param runtime 内部运行容器 @param properties 框架运行策略 @param models 授权模型入口
     * @param preparation 业务准备实现 @param knowledge 业务检索实现 @param records 业务记录实现
     * @return 业务统一调用入口，同一应用各接口须具有唯一可注入实现
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({AgentChatPrepareService.class, AgentKnowledgeQueryService.class,
            AgentRunRecordService.class, ModelClientRegistry.class})
    public AgentChatService agentChatService(AgentRuntime runtime, AgentRuntimeProperties properties,
            ModelClientRegistry models, AgentChatPrepareService preparation,
            AgentKnowledgeQueryService knowledge, AgentRunRecordService records) {
        return new AgentChatService(runtime, properties, models, preparation, knowledge, records);
    }
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
