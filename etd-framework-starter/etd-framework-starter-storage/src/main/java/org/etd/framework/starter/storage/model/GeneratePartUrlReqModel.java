package org.etd.framework.starter.storage.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 申请分片直传 URL 请求模型
 *
 * @author Young
 */
@Data
public class GeneratePartUrlReqModel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 存储桶内的对象 Key/相对路径（InitMultipart 返回的 fileName）
     */
    private String fileName;

    /**
     * 分片上传任务唯一标识 ID（InitMultipart 返回的 uploadId）
     */
    private String uploadId;

    /**
     * 分片序号（从 1 开始，如 1, 2, 3...）
     */
    private Integer partNumber;

    /**
     * 分片直传链接有效时长（单位：秒；可选）
     */
    private Integer expiry;
}
