package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.InputStream;
import java.io.Serializable;

/**
 * 服务端 InputStream 流上传请求模型
 *
 * @author Young
 */
@Data
public class InputStreamUploadReqModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 存储目录（如 user/avatar）
     */
    private String directory;

    /**
     * 原始文件名（用于自动提取后缀生成 UUID 文件名）
     */
    private String originalFileName;

    /**
     * 指定最终存盘文件名（可选）
     */
    private String fileName;

    /**
     * 物理输入流
     */
    private InputStream inputStream;

    /**
     * 流总字节大小（可选，不传默认 -1 自动使用分片机制）
     */
    private long fileSize = -1L;

    /**
     * 文件 MIME 类型
     */
    private String contentType;
}
