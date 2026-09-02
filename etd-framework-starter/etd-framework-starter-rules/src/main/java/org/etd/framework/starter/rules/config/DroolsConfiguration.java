package org.etd.framework.starter.rules.config;


import org.etd.framework.starter.rules.manage.RulesManage;
import org.etd.framework.starter.rules.manage.exdent.DBRulesManage;
import org.etd.framework.starter.rules.properties.RulesProperties;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Drools 规则引擎基础 Spring 自动配置类。
 */
@EnableConfigurationProperties(value = RulesProperties.class)
@Configuration
public class DroolsConfiguration {

    /**
     * kie Services
     *
     * @return 处理结果
     */
    @Bean
    public KieServices kieServices() {
        return KieServices.get();
    }

    /**
     * kie File System
     *
     * @param kieServices 参数 kieServices
     * @return 处理结果
     */
    @Bean
    public KieFileSystem kieFileSystem(KieServices kieServices) {
        return kieServices.newKieFileSystem();
    }

    /**
     * kie Module
     *
     * @param kieServices 参数 kieServices
     * @return 处理结果
     */
    @Bean
    public KieModuleModel kieModule(KieServices kieServices) {
        return kieServices.newKieModuleModel();
    }

    /**
     * kie Container
     *
     * @param kieServices 参数 kieServices
     * @return 处理结果
     */
    @Bean
    public KieContainer kieContainer(KieServices kieServices) {
        return kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }


    /**
     * db Rules Context
     *
     * @param kieServices 参数 kieServices
     * @param kieFileSystem 参数 kieFileSystem
     * @param kieModuleModel 参数 kieModuleModel
     * @param properties 参数 properties
     * @return 处理结果
     */
    @ConditionalOnProperty(prefix = "rules.dbRules", value = "enabled")
    @Bean
    public RulesManage dbRulesContext(KieServices kieServices, KieFileSystem kieFileSystem, KieModuleModel kieModuleModel, RulesProperties properties) {
        return new DBRulesManage(kieServices, kieFileSystem, kieModuleModel, properties);
    }

    /**
     * rules Context
     *
     * @param kieServices 参数 kieServices
     * @param kieFileSystem 参数 kieFileSystem
     * @param kieModuleModel 参数 kieModuleModel
     * @param properties 参数 properties
     * @return 处理结果
     */
    @ConditionalOnMissingBean(RulesManage.class)
    @Bean
    public RulesManage rulesContext(KieServices kieServices, KieFileSystem kieFileSystem, KieModuleModel kieModuleModel, RulesProperties properties) {
        return new RulesManage(kieServices, kieFileSystem, kieModuleModel, properties);
    }


}
