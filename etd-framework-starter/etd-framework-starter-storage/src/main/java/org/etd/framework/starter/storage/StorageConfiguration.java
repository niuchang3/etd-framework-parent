package org.etd.framework.starter.storage;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import io.minio.MinioClient;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.core.extend.AlibabaOSSFileStorage;
import org.etd.framework.starter.storage.core.extend.MinIoFileStorage;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 存储服务 Spring Boot 自动配置类
 *
 * @author Young
 */
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
@Import(StorageContext.class)
public class StorageConfiguration {

    /**
     * 创建 MinIO SDK 客户端
     */
    @ConditionalOnProperty(prefix = "storage.minio", value = "enabled", havingValue = "true")
    @Bean
    public MinioClient minioClient(StorageProperties storageProperties) {
        return MinioClient.builder()
                .endpoint(storageProperties.getMinio().getEndpoint())
                .credentials(storageProperties.getMinio().getAccessKey(), storageProperties.getMinio().getSecretKey())
                .build();
    }

    /**
     * 创建 MinIO 文件存储策略实现
     */
    @ConditionalOnBean(MinioClient.class)
    @Bean
    public FileStorage minIoFileStorage(StorageProperties storageProperties) {
        return new MinIoFileStorage(storageProperties.getMinio());
    }

    /**
     * 创建阿里云 OSS SDK 客户端
     */
    @ConditionalOnProperty(prefix = "storage.alibaba", value = "enabled", havingValue = "true")
    @Bean
    public OSSClient ossClient(StorageProperties storageProperties) {
        return (OSSClient) new OSSClientBuilder().build(
                storageProperties.getAlibaba().getEndpoint(),
                storageProperties.getAlibaba().getAccessKey(),
                storageProperties.getAlibaba().getSecretKey()
        );
    }

    /**
     * 创建阿里云 OSS 文件存储策略实现
     */
    @ConditionalOnBean(OSSClient.class)
    @Bean
    public FileStorage alibabaFileStorage(StorageProperties storageProperties) {
        return new AlibabaOSSFileStorage(storageProperties.getAlibaba());
    }
}
