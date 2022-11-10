package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

@Data
public abstract class FileUploadModel<E> implements Serializable {

    /**
     * 上传的桶名称
     */
    protected String bucketName;
    /**
     * 文件存放的目录
     */
    protected String directory;

    public abstract E getFile();
}
