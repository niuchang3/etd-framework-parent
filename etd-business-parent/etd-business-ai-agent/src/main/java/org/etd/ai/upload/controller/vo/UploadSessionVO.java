package org.etd.ai.upload.controller.vo;

import lombok.Data;

import java.time.Instant;

/** UploadSessionVO 页面响应；所有业务ID按框架Long序列化约定传输。 */
@Data
public class UploadSessionVO {
    /** 后端上传任务ID，不是MinIO任务ID。 */
    private Long taskId;

    /** 固定分片字节数。 */
    private Long partSizeBytes;

    /** 固定总分片数。 */
    private Integer partCount;

    /** 组件并发上限。 */
    private Integer concurrency;

    /** 失败额外重试次数。 */
    private Integer maxRetries;

    /** 单片签名秒数。 */
    private Integer urlExpirySeconds;

    /** 任务截止时间。 */
    private Instant expiresAt;
}
