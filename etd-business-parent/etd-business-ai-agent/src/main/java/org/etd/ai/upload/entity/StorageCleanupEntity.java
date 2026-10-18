package org.etd.ai.upload.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.etd.framework.starter.mybaits.core.BaseEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

import java.time.Instant;

/** StorageCleanupEntity 持久化记录；租户及审计字段由框架可信上下文填充。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_storage_cleanup")
public class StorageCleanupEntity extends BaseEntity {
    /** 待删除对象桶。 */
    private String bucket;

    /** 待删除对象键。 */
    private String objectKey;

    /** 待中止任务ID，空时按对象键查找未完成任务。 */
    private String uploadId;

    /** 是否中止未完成任务。 */
    private Boolean abortMultipart;

    /** 下次可重试时间。 */
    private Instant nextAttemptAt;

    /** 已失败次数。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Integer attempts;
}
