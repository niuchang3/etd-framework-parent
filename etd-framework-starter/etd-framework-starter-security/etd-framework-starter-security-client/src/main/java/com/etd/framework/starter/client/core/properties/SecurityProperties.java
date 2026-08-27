package com.etd.framework.starter.client.core.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 内部安全认证配置项。
 */
@Data
@ConfigurationProperties(prefix = "system.security")
public class SecurityProperties {

    /**
     * 发行人
     */
    private String issuer = "NiuChang";

    /**
     * 访问令牌配置。
     */
    @NestedConfigurationProperty
    private Token accessToken = new Token("/internal/login",ChronoUnit.MINUTES, 60L, true);

    /**
     * 刷新令牌配置。
     */
    @NestedConfigurationProperty
    private Token refreshToken = new Token("/internal/refresh",ChronoUnit.DAYS, 7L, true);

    /**
     * 退出登录配置。
     */
    @NestedConfigurationProperty
    private Endpoint logout = new Endpoint("/internal/logout", true);

    /**
     * 权限相关配置。
     */
    @NestedConfigurationProperty
    private Permissions permissions = new Permissions();

    /**
     * JWT 签名密钥配置。
     */
    @NestedConfigurationProperty
    private Key key = new Key();


    /**
     * 令牌生命周期配置。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Token {
        /**
         * 请求地址
         */
        private String endpoint;

        /**
         * 时间单位。
         */
        private ChronoUnit timeUnit;

        /**
         * 过期时长。
         */
        private Long expired;

        /**
         * 是否启用。
         */
        private Boolean enabled;
    }

    /**
     * 内部安全端点配置。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Endpoint {

        /**
         * 请求地址。
         */
        private String endpoint;

        /**
         * 是否启用。
         */
        private Boolean enabled;
    }

    /**
     * 权限配置。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Permissions {

        /**
         * 放行路径。
         */
        private List<String> ignore;
    }

    /**
     * JWT 签名密钥配置。
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Key {

        /**
         * RSA 公钥文件路径。
         */
        private String publicKeyPath = "conf/rsaPublicKey.pem";

        /**
         * RSA 私钥文件路径。
         */
        private String privateKeyPath = "conf/rsaPrivateKey.pem";
    }

}
