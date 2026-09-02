package org.etd.framework.starter.storage;

import lombok.Getter;
import org.etd.framework.starter.storage.core.FileDownload;
import org.etd.framework.starter.storage.core.FileStorage;
import org.etd.framework.starter.storage.core.FileUpload;
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

    /**
     * 设置 ApplicationContext 属性值
     *
     * @param applicationContext 参数 applicationContext
     */
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
     * 根据类型获取对应的通用存储策略实现 (组合包含上传与下载)
     *
     * @param clientType 客户端类型
     * @return 对应的存储策略 Bean
     */
    /**
     * 获取 StorageClient 属性值
     *
     * @param clientType 参数 clientType
     * @return 处理结果
     */
    public static FileStorage getStorageClient(ClientType clientType) {
        Object bean = getBean(clientType.getStorageClient());
        if (ObjectUtils.isEmpty(bean)) {
            throw new RuntimeException("Bean not initialized, please check if " + clientType.getStorageClient().getName() + " is managed by Spring");
        }
        return (FileStorage) bean;
    }

    /**
     * 根据类型获取对应的上传策略实现
     *
     * @param clientType 客户端类型
     * @return 对应的上传策略 Bean
     */
    /**
     * 获取 UploadClient 属性值
     *
     * @param clientType 参数 clientType
     * @return 处理结果
     */
    public static FileUpload getUploadClient(ClientType clientType) {
        return getStorageClient(clientType);
    }

    /**
     * 根据类型获取对应的下载与外链生成策略实现
     *
     * @param clientType 客户端类型
     * @return 对应的下载策略 Bean
     */
    /**
     * 获取 DownloadClient 属性值
     *
     * @param clientType 参数 clientType
     * @return 处理结果
     */
    public static FileDownload getDownloadClient(ClientType clientType) {
        return getStorageClient(clientType);
    }

    private static <T> T getBean(Class<?> aClass) {
        return (T) getApplicationContext().getBean(aClass);
    }
}
