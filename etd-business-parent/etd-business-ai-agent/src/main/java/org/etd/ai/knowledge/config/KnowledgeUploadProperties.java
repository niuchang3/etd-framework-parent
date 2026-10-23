package org.etd.ai.knowledge.config;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.etd.ai.knowledge.constant.KnowledgePolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 上传技术边界；允许扩展名不在服务端配置或校验。 */
@Data
@Validated
@ConfigurationProperties("etd.ai.knowledge.upload")
public class KnowledgeUploadProperties {
    /** 是否启用主表驱动的过期上传清理，默认启用。 */
    private boolean cleanupEnabled = true;
    /** 原件大小上限，默认1GiB。 */
    @Min(1)
    private long maxFileBytes = 1024L * 1024 * 1024;
    /** 固定分片大小，默认10MiB。 */
    @Min(KnowledgePolicy.MIN_PART_BYTES) @Max(5368709120L)
    private long partSizeBytes = 10L * 1024 * 1024;
    /** 上传会话有效期，默认24小时，包含续传时间。 */
    @Min(60)
    private long sessionSeconds = 86400;
    /** 临时上传和下载地址有效期，默认15分钟。 */
    @Min(1) @Max(604800)
    private int urlSeconds = 900;
}
