package org.etd.framework.starter.storage.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 抽象文件存储策略基类
 *
 * @param <C> 底层 SDK 客户端类型
 * @author Young
 */
public abstract class FileStorage<C> implements FileUpload {

    @Autowired
    private C client;

    protected C getClient() {
        return client;
    }

    protected static final String SEPARATOR_DOT = ".";
    protected static final String SEPARATOR_DASH = "-";
    protected static final String SEPARATOR_EMPTY = "";
    protected static final String SEPARATOR_SLASH = "/";

    /**
     * 生成随机防重名的上传文件名
     *
     * @param originalFileName 原始文件名
     * @return 格式为 UUID + 后缀 的文件名
     */
    protected String generateFileName(String originalFileName) {
        String suffix = originalFileName;
        if (StringUtils.hasText(originalFileName) && originalFileName.contains(SEPARATOR_DOT)) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf(SEPARATOR_DOT));
        }
        return UUID.randomUUID().toString().replace(SEPARATOR_DASH, SEPARATOR_EMPTY).toUpperCase() + suffix;
    }

    /**
     * 构建在 Bucket 内部的安全对象存储路径（防止 directory 为空时拼出开头带 / 的根路径）
     *
     * @param directory 目标目录
     * @param fileName  文件名称
     * @return 规范的对象 Key/Path
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
