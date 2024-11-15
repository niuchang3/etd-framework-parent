package com.etd.framework.starter.client.core.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "system.oauth")
public class SystemOauthProperties {

    /**
     * 发行人
     */
    private String issuer = "NiuChang";


    /**
     * accessToken 配置
     */
    @NestedConfigurationProperty
    private Token accessToken = new Token(ChronoUnit.MINUTES, 30L, true);

    /**
     * refreshToken 配置
     */
    @NestedConfigurationProperty
    private Token refreshToken = new Token(ChronoUnit.MINUTES, 60L, true);

    @NestedConfigurationProperty
    private Permissions permissions = new Permissions();


    /**
     * Token 相关配置
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class Token {


        private ChronoUnit timeUnit;

        private Long expired;

        private Boolean enabled;
    }

    /**
     * 权限
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class Permissions {

        private List<String> ignore;
    }

}
