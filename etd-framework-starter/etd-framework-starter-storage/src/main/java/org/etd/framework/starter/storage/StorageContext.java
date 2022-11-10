package org.etd.framework.starter.storage;

import lombok.Getter;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.core.exdent.AlibabaOSSFileStorage;
import org.etd.framework.starter.storage.core.exdent.MinIoFileStorage;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
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
         * 以MinIO实现存储
         */
        MinIo(MinIoFileStorage.class),
        /**
         * 上传到阿里巴巴服务
         */
        AlibabaOSS(AlibabaOSSFileStorage.class);

        @Getter
        private Class<?> storageClient;

        ClientType(Class<?> storageClient) {
            this.storageClient = storageClient;
        }
    }

    /**
     * 根据类型获取对应的客户端实现
     *
     * @param clientType
     */
    public static FileStorage getStorageClient(ClientType clientType) {
        Object bean = getBean(clientType.getStorageClient());
        if (ObjectUtils.isEmpty(bean)) {
            throw new RuntimeException("Bean not initialized,Inspection " + clientType.getStorageClient().getName() + " Is it managed by Spring");
        }
        return (FileStorage) bean;
    }

    private static <T> T getBean(Class<?> aClass) {
        return (T) getApplicationContext().getBean(aClass);
    }
}
