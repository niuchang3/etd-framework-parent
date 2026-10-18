package org.etd.ai.document.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.apache.ibatis.type.ObjectTypeHandler;
import org.etd.framework.starter.mybaits.core.OrgScopedEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

import java.time.Instant;
import java.util.UUID;

/** KnowledgeDocumentEntity 持久化记录；租户及审计字段由框架可信上下文填充。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_knowledge_document", autoResultMap = true)
public class KnowledgeDocumentEntity extends OrgScopedEntity {
    /** 非负乐观锁版本，修改时必须提交当前值。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    @Version
    private Long version;

    /** 所属知识库。 */
    private Long knowledgeBaseId;

    /** 所属目录，空表示未分类。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long folderId;

    /** 显示名称。 */
    private String documentName;

    /** 原始文件名。 */
    private String originalFilename;

    /** 后端存储桶，不向页面返回。 */
    private String storageBucket;

    /** 后端对象键，不向页面返回。 */
    private String storageObjectKey;

    /** 后端按原始文件名推断的内容类型，不表示已验证文件格式。 */
    private String contentType;

    /** 原始文件字节数。 */
    private Long fileSize;

    /** 文件SHA256摘要。 */
    private String fileSha256;

    /** 是否租户内公开读取。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("false")
    private Boolean isPublic;

    /** 文件存储实现。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("'minio'")
    private String storageProvider;

    /** 仅上传完成，不表示解析成功。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("'uploaded'")
    private String processingStatus;

    /** 当前处理任务的租约标识；替换原件时清空，避免旧任务继续发布。 */
    // PostgreSQL驱动原生返回UUID，显式使用getObject/setObject，避免自动映射忽略该类型。
    @TableField(typeHandler = ObjectTypeHandler.class)
    private UUID processingOwner;

    /** 当前处理任务的租约到期时间点。 */
    private Instant processingLeaseUntil;

    /** 本轮处理任务开始的时间点。 */
    private Instant processingStartedTime;

    /** 本轮实际使用的解析器及规则版本，最长64个字符。 */
    private String parserVersion;

    /** 本轮实际使用的分词器及分片规则版本，最长64个字符。 */
    private String splitterVersion;

    /** 本轮实际分片的Token上限快照，设置时必须大于0。 */
    private Integer chunkMaxTokens;

    /** 本轮实际分片的重叠Token数，非负且小于分片上限。 */
    private Integer chunkOverlapTokens;

    /** 最近一次处理失败的脱敏摘要，最长1000个字符，禁止包含凭证。 */
    private String errorMessage;

    /** 本轮处理成功或失败的结束时间点。 */
    private Instant processedTime;

    /** 文件内容处理轮次。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Long processingVersion;
}
