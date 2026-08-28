package com.etd.framework.starter.oauth.authentication.oauth2.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2授权服务器配置项。
 */
@Data
@ConfigurationProperties(prefix = "system.security.oauth2.authorization-server")
public class OAuth2AuthorizationServerProperties {

    /**
     * 是否启用标准OAuth2授权服务器。
     */
    private Boolean enabled = false;

    /**
     * 授权服务器发行人地址。
     */
    private String issuer;

}
