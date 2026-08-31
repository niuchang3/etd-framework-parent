package org.etd.framework.starter.storage.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置属性
 *
 * @author Young
 */
@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * MinIO 存储配置
     */
    private MinIo minio = new MinIo();

    /**
     * 阿里云 OSS 存储配置
     */
    private AlibabaOSS alibaba = new AlibabaOSS();

    @Data
    public static class MinIo {
        private boolean enabled;
        private String endpoint;
        private String accessKey;
        private String secretKey;
        /**
         * 默认全局存储桶名称
         */
        private String defaultBucket;
        /**
         * 自动建桶时的默认访问策略（默认 private）。可选值：
         * 1. private: 私有读写（默认，最高安全等级，禁止匿名读写）
         * 2. public-read (或 public): 公共读、私有写（适用于公开图片、头像、静态文件）
         * 3. public-read-write: 公共读写（高危，允许任何人匿名读、写、删除，请谨慎使用）
         * 4. authenticated-read: 授权读（仅通过身份认证的账号可读）
         */
        private String policy = "private";
        /**
         * 预签名链接默认有效时长（单位：秒，默认 7 天）
         */
        private int expiry = 604800;
    }

    @Data
    public static class AlibabaOSS {
        private boolean enabled;
        private String endpoint;
        private String accessKey;
        private String secretKey;
        /**
         * 默认全局存储桶名称
         */
        private String defaultBucket;
        /**
         * 自动建桶时的默认访问策略（默认 private）。可选值：
         * 1. private: 私有读写（默认，最高安全等级，禁止匿名读写）
         * 2. public-read (或 public): 公共读、私有写（适用于公开图片、头像、静态文件）
         * 3. public-read-write: 公共读写（高危，允许任何人匿名读、写、删除，请谨慎使用）
         * 4. authenticated-read: 授权读（仅通过身份认证的账号可读）
         */
        private String policy = "private";
        /**
         * 预签名链接默认有效时长（单位：秒，默认 7 天）
         */
        private int expiry = 604800;
    }
}
