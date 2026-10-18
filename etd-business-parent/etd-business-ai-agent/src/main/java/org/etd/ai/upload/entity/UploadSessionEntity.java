package org.etd.ai.upload.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

import java.time.Instant;

/** UploadSessionEntity 持久化记录；租户及审计字段由框架可信上下文填充。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_file_upload")
public class UploadSessionEntity extends BaseEntity {
    /** 非负乐观锁版本，修改时必须提交当前值。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    @Version
    private Long version;

    /** 所属知识库。 */
    private Long knowledgeBaseId;

    /** 目标目录，空为未分类。 */
    private Long folderId;

    /** 可选被替换文件ID。 */
    private Long replaceDocumentId;

    /** 替换时预期文件版本。 */
    private Long replaceVersion;

    /** 原始文件名。 */
    private String originalFilename;

    /** 声明文件字节数。 */
    private Long fileSize;

    /** 后端确定的存储桶。 */
    private String bucket;

    /** 唯一对象键。 */
    private String objectKey;

    /** MinIO真实分片任务ID，不返回前端。 */
    private String uploadId;

    /** 初始化、上传中、已完成或已取消。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("T(org.etd.ai.upload.constant.UploadState).INITIALIZING")
    private String state;

    /** 幂等完成后登记的文件标识。 */
    private Long documentId;

    /** 任务固定分片字节数。 */
    private Long partSizeBytes;

    /** 预期分片数。 */
    private Integer partCount;

    /** 签名秒数。 */
    private Integer urlExpirySeconds;

    /** 前端最大并发。 */
    private Integer concurrency;

    /** 前端重试次数。 */
    private Integer maxRetries;

    /** 任务绝对过期时间。 */
    private Instant expiresAt;
}
