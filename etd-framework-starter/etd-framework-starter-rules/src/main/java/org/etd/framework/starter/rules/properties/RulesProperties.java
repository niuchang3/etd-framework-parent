package org.etd.framework.starter.rules.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * 规则引擎配置属性类。
 */
@ConfigurationProperties(prefix = "rules")
@Data
public class RulesProperties {


    private String path;

    @NestedConfigurationProperty
    private DbRules dbRules;

    @Data
    public class DbRules {

        private Boolean enabled = false;

    }
}
