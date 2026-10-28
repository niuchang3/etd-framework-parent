package org.etd.ai.agent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 业务聊天运行策略配置，不改变MVC应用类型或现有安全路径。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentChatProperties.class)
public class AgentChatConfiguration {
}
