package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 服务端 byte[] 字节数组上传请求模型
 *
 * @author Young
 */
@Data
public class ByteUploadReqModel implements Serializable {

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
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 指定最终存盘文件名（可选）
     */
    private String fileName;

    /**
     * 物理字节数组
     */
    private byte[] bytes;

    /**
     * 文件 MIME 类型
     */
    private String contentType;
}
