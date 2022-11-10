package org.etd.framework.starter.storage;


import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import io.minio.MinioClient;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.core.exdent.AlibabaOSSFileStorage;
import org.etd.framework.starter.storage.core.exdent.MinIoFileStorage;
import org.etd.framework.starter.storage.properties.StorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@EnableConfigurationProperties(StorageProperties.class)
@ConfigurationPropertiesScan("org.etd.framework.starter.storage.**")
@ComponentScan("org.etd.framework.starter.storage.**")
@Configuration
public class StorageConfiguration {

    /**
     * 创建Minio客户端
     *
     * @param storageProperties
     * @return
     */
    @ConditionalOnProperty(prefix = "storage.minio", value = "enabled")
    @Bean
    public MinioClient minioClient(@Autowired StorageProperties storageProperties) {
        return MinioClient.builder()
                .endpoint(storageProperties.getMinio().getEndpoint())
                .credentials(storageProperties.getMinio().getAccessKey(), storageProperties.getMinio().getSecretKey())
                .build();
    }

    /**
     * 创建MinioBean
     *
     * @param storageProperties
     * @return
     */
    @ConditionalOnBean(MinioClient.class)
    @Bean
    public FileStorage minIoFileStorage(@Autowired StorageProperties storageProperties) {
        return new MinIoFileStorage(storageProperties.getMinio());
    }

    @ConditionalOnProperty(prefix = "storage.alibaba", value = "enabled")
    @Bean
    public OSSClient ossClient(@Autowired StorageProperties storageProperties) {
        return (OSSClient) new OSSClientBuilder()
                .build(storageProperties.getAlibaba().getEndpoint(), storageProperties.getAlibaba().getAccessKey(), storageProperties.getAlibaba().getSecretKey());
    }

    /**
     * 创建AlibabaOSS
     *
     * @param storageProperties
     * @return
     */
    @ConditionalOnBean(OSSClient.class)
    @Bean
    public FileStorage alibabaFileStorage(@Autowired StorageProperties storageProperties) {
        return new AlibabaOSSFileStorage(storageProperties.getAlibaba());
    }
}
