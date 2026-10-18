package org.etd.ai.model.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** SaaS服务端凭证加密配置，不能保存到数据库或响应。 */
@Data
@ConfigurationProperties("etd.ai.credentials")
public class ModelCredentialProperties {
    /** Base64编码的32字节AES密钥；本地配置提供开发默认值，其他环境必须从运行环境注入。 */
    private String key;

    /** 当前主密钥标识，单主密钥阶段不提供历史密钥兼容链。 */
    private String keyId;
}
