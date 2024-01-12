package com.etd.framework.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "oauth2.authorization")
@Data
public class AuthorizationServerKeyPair {

    private String publicKey;

    private String privateKey;
}
