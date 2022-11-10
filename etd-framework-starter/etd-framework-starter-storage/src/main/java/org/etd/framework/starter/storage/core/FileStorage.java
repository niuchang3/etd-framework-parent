package org.etd.framework.starter.storage.core;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

public abstract class FileStorage<C> implements FileUpload {

    /**
     * 上传的客户端,该客户端需要被Spring管理
     * 目前支持Minio
     */
    @Autowired
    private C client;

    protected C getClient() {
        return client;
    }


    protected static final String SEPARATOR_DOT = ".";

    protected static final String SEPARATOR_ACROSS = "-";

    protected static final String SEPARATOR_STR = "";

    protected static final String SEPARATOR_BAR = "/";


    /**
     * @param originalFileName
     * @return java.lang.String
     * @Description 生成上传文件名
     * @author exe.wangtaotao
     * @date 2020/10/21 15:07
     */
    protected String generateFileName(String originalFileName) {
        String suffix = originalFileName;
        if (originalFileName.contains(SEPARATOR_DOT)) {
            suffix = originalFileName.substring(originalFileName.lastIndexOf(SEPARATOR_DOT));
        }
        return UUID.randomUUID().toString().replace(SEPARATOR_ACROSS, SEPARATOR_STR).toUpperCase() + suffix;
    }
}
