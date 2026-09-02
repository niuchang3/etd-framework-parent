package org.etd.framework.starter.storage.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 抽象文件存储策略基类（同时实现上传与下载/预览能力）
 *
 * @param <C> 底层 SDK 客户端类型
 * @author Young
 */
public abstract class FileStorage<C> implements FileUpload, FileDownload {

    @Autowired
    private C client;

    /**
     * 获取 Client 属性值
     *
     * @return 处理结果
     */
    protected C getClient() {
        return client;
    }

    protected static final String SEPARATOR_DOT = ".";
    protected static final String SEPARATOR_DASH = "-";
    protected static final String SEPARATOR_EMPTY = "";
    protected static final String SEPARATOR_SLASH = "/";

    /**
     * 确保存储桶存在（如果不存在则自动创建）
     *
     * @param bucketName 存储桶名称
     * @throws Exception 异常信息
     */
    /**
     * ensure Bucket Exists
     *
     * @param bucketName 参数 bucketName
     * @return 处理结果
     */
    protected abstract void ensureBucketExists(String bucketName) throws Exception;

    /**
     * 安全获取目标存储桶名称（请求中未传时自动使用默认配置桶）
     *
     * @param requestBucketName       请求中传入的桶名（可能为空）
     * @param defaultPropertiesBucket 配置中指定的默认桶名
     * @return 最终生效的存储桶名称
     */
    /**
     * 获取 RealBucketName 属性值
     *
     * @param requestBucketName 参数 requestBucketName
     * @param defaultPropertiesBucket 参数 defaultPropertiesBucket
     * @return 处理结果
     */
    protected String getRealBucketName(String requestBucketName, String defaultPropertiesBucket) {
        if (StringUtils.hasText(requestBucketName)) {
            return requestBucketName.trim();
        }
        if (StringUtils.hasText(defaultPropertiesBucket)) {
            return defaultPropertiesBucket.trim();
        }
        throw new IllegalArgumentException("BucketName cannot be empty! Please provide bucketName in request or configure 'default-bucket' in application.yml");
    }

    /**
     * 生成随机防重名的上传文件名
     *
     * @param originalFileName 原始文件名
     * @return 格式为 UUID + 后缀 的文件名
     */
    /**
     * 生成 File Name
     *
     * @param originalFileName 参数 originalFileName
     * @return 处理结果
     */
    protected String generateFileName(String originalFileName) {
        String suffix = originalFileName;
        if (StringUtils.hasText(originalFileName) && originalFileName.contains(SEPARATOR_DOT)) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf(SEPARATOR_DOT));
        }
        return UUID.randomUUID().toString().replace(SEPARATOR_DASH, SEPARATOR_EMPTY).toUpperCase() + suffix;
    }

    /**
     * 构建在 Bucket 内部的安全对象存储路径
     *
     * @param directory 目标目录
     * @param fileName  文件名称
     * @return 规范的对象 Key/Path
     */
    /**
     * 构建 Object Path
     *
     * @param directory 参数 directory
     * @param fileName 参数 fileName
     * @return 处理结果
     */
    protected String buildObjectPath(String directory, String fileName) {
        if (!StringUtils.hasText(directory)) {
            return fileName;
        }
        String cleanDirectory = directory.trim();
        if (cleanDirectory.startsWith(SEPARATOR_SLASH)) {
            cleanDirectory = cleanDirectory.substring(1);
        }
        if (cleanDirectory.endsWith(SEPARATOR_SLASH)) {
            return cleanDirectory + fileName;
        }
        return cleanDirectory + SEPARATOR_SLASH + fileName;
    }
}
