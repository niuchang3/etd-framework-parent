package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 直传 URL 申请请求模型
 *
 * @author Young
 */
@Data
public class UploadUrlReqModel implements Serializable {

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
     * 原始文件名（用于自动提取后缀并结合 UUID 生成新文件名）
     */
    private String originalFileName;

    /**
     * 指定最终存盘文件名（可选；若不填则基于 originalFileName 自动生成 UUID 文件名）
     */
    private String fileName;

    /**
     * 文件 MIME 类型（如 image/png, video/mp4）
     */
    private String contentType;

    /**
     * 预签名链接有效时长（单位：秒；可选，不填默认使用配置的 expiry）
     */
    private Integer expiry;
}
