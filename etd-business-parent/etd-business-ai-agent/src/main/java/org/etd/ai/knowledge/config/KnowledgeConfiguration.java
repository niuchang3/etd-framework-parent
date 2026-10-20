package org.etd.ai.knowledge.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 启用资料上传的类型安全参数绑定及边界校验。 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(KnowledgeUploadProperties.class)
public class KnowledgeConfiguration {}
