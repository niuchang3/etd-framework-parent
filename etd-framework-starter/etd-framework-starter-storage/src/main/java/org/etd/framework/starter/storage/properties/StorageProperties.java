package org.etd.framework.starter.storage.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {


    public StorageProperties() {

    }

    /**
     * MinIo存储
     */
    @NestedConfigurationProperty
    private MinIo minio;
    /**
     * 阿里巴巴OSS存储
     */
    @NestedConfigurationProperty
    private AlibabaOSS alibaba;

    /**
     * MinIo 配置
     */
    @Data
    public static class MinIo {

        private boolean enabled;
        /**
         * Minio连接端点  http://127.0.0.1:9000
         */
        private String endpoint = "http://127.0.0.1:9000";

        private String accessKey = "minioadmin";

        private String secretKey = "minioadmin";
        /**
         * 是否自动创建bucket
         */
        private Boolean autoCreateBucket = false;
        /**
         * 过期时间默认最大7天,单位秒
         */
        private Integer expiry = 604800;
    }

    @Data
    public static class AlibabaOSS {

        private boolean enabled;
        /**
         * 连接端点  http://127.0.0.1:9000
         */
        private String endpoint = "";

        private String accessKey = "";

        private String secretKey = "";
        /**
         * 是否自动创建bucket
         */
        private Boolean autoCreateBucket = false;
        /**
         * 过期时间默认最大7天,单位秒
         */
        private Integer expiry = 604800;
    }
}
