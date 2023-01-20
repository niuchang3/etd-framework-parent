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

@EnableConfigurationProperties(value = RulesProperties.class)
@Configuration
public class DroolsConfiguration {

    @Bean
    public KieServices kieServices() {
        return KieServices.get();
    }

    @Bean
    public KieFileSystem kieFileSystem(KieServices kieServices) {
        return kieServices.newKieFileSystem();
    }

    @Bean
    public KieModuleModel kieModule(KieServices kieServices) {
        return kieServices.newKieModuleModel();
    }

    @Bean
    public KieContainer kieContainer(KieServices kieServices) {
        return kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
    }


    @ConditionalOnProperty(prefix = "rules.dbRules", value = "enabled")
    @Bean
    public RulesManage dbRulesContext(KieServices kieServices, KieFileSystem kieFileSystem, KieModuleModel kieModuleModel, RulesProperties properties) {
        return new DBRulesManage(kieServices, kieFileSystem, kieModuleModel, properties);
    }

    @ConditionalOnMissingBean(RulesManage.class)
    @Bean
    public RulesManage rulesContext(KieServices kieServices, KieFileSystem kieFileSystem, KieModuleModel kieModuleModel, RulesProperties properties) {
        return new RulesManage(kieServices, kieFileSystem, kieModuleModel, properties);
    }


}
