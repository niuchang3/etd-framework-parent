package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 初始化分片任务请求模型
 *
 * @author Young
 */
@Data
public class InitMultipartReqModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 存储目录
     */
    private String directory;

    /**
     * 原始文件名（用于自动生成 UUID 文件名）
     */
    private String originalFileName;

    /**
     * 指定最终存盘文件名（可选）
     */
    private String fileName;

    /**
     * 文件 Content-Type
     */
    private String contentType;
}
