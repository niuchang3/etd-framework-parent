package org.etd.framework.starter.storage;

import lombok.Getter;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.core.extend.AlibabaOSSFileStorage;
import org.etd.framework.starter.storage.core.extend.MinIoFileStorage;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ObjectUtils;

/**
 * 存储上下文工具类
 *
 * @author Young
 */
public class StorageContext implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        StorageContext.applicationContext = applicationContext;
    }

    private static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public enum ClientType {
        /**
         * 以 MinIO 实现存储
         */
        MinIo(MinIoFileStorage.class),
        /**
         * 上传到阿里云 OSS 服务
         */
        AlibabaOSS(AlibabaOSSFileStorage.class);

        @Getter
        private final Class<?> storageClient;

        ClientType(Class<?> storageClient) {
            this.storageClient = storageClient;
        }
    }

    /**
     * 根据类型获取对应的客户端实现
     *
     * @param clientType 客户端类型
     * @return 对应的存储策略 Bean
     */
    public static FileStorage getStorageClient(ClientType clientType) {
        Object bean = getBean(clientType.getStorageClient());
        if (ObjectUtils.isEmpty(bean)) {
            throw new RuntimeException("Bean not initialized, please check if " + clientType.getStorageClient().getName() + " is managed by Spring");
        }
        return (FileStorage) bean;
    }

    private static <T> T getBean(Class<?> aClass) {
        return (T) getApplicationContext().getBean(aClass);
    }
}
