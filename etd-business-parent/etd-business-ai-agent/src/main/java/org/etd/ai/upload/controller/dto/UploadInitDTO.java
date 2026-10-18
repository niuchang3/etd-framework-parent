package org.etd.ai.upload.controller.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

import org.etd.ai.upload.constant.MultipartUploadLimits;

/** UploadInitDTO 页面请求；不接收租户、创建人或存储对象键。 */
@Data
public class UploadInitDTO {
    /** 原始文件显示名称，最多255字符，不用作存储路径；扩展名白名单由客户端校验。 */
    @NotBlank
    @Size(max = 255)
    private String originalFilename;

    /** 声明字节数。 */
    @NotNull
    @Min(1)
    private Long fileSize;

    /** 客户端读取系统参数后提交的普通分片字节数，必须满足S3分片协议。 */
    @NotNull
    @Min(MultipartUploadLimits.MIN_PART_BYTES)
    @Max(MultipartUploadLimits.MAX_PART_BYTES)
    private Long partSizeBytes;

    /** 客户端整个上传队列使用的并发数，服务端仅保存本次快照。 */
    @NotNull
    @Positive
    private Integer concurrency;

    /** 客户端每片首次失败后的额外重试次数，允许为0。 */
    @NotNull
    @PositiveOrZero
    private Integer maxRetries;

    /** 客户端申请的分片签名有效秒数，不能超过S3预签名有效期。 */
    @NotNull
    @Positive
    @Max(MultipartUploadLimits.MAX_URL_EXPIRY_SECONDS)
    private Integer urlExpirySeconds;

    /** 客户端指定的任务有效秒数，服务端据此保存绝对过期时间。 */
    @NotNull
    @Positive
    private Integer sessionExpireSeconds;

    /** 目标目录，空表示未分类。 */
    @Positive private Long folderId;

    /** 替换文件ID，空表示新文件。 */
    @Positive private Long replaceDocumentId;

    /** 替换时必填的当前文件版本。 */
    @Min(0)
    private Long replaceVersion;
}
