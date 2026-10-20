package org.etd.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import org.etd.ai.knowledge.mapper.typehandler.UploadPartsTypeHandler;
import org.etd.ai.knowledge.mapper.typehandler.ProcessingOwnerTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.ai.knowledge.model.UploadPart;
import org.etd.framework.starter.mybaits.core.OrgScopedEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** 资料兼上传主记录；原件、正文分片和向量的生命周期以此记录为准。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_knowledge_document", autoResultMap = true)
public class KnowledgeDocumentEntity extends OrgScopedEntity {
    /** 乐观锁版本，默认0，编辑时必须携带。 */
    @Version
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Long version;

    /** 所属分类目录ID。 */
    private Long folderId;

    /** 资料显示名称，最多255字。 */
    private String documentName;

    /** 资料用途说明，最多1000字，可清空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    /** 是否允许租户内公开检索，不授予维护权限。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("false")
    private Boolean isPublic;

    /** 检索方式：keyword、vector、hybrid。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("T(org.etd.ai.knowledge.constant.KnowledgePolicy).KEYWORD")
    private String retrievalMode;

    /** 向量模型连接器ID，全文模式可不配置。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long embeddingModelConnectionId;

    /** 平台向量模型ID，由后端根据连接器确定。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long embeddingModelId;

    /** 向量维度，仅允许768、1024或1536。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer embedding;

    /** 单个正文分片的Token上限。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("500")
    private Integer chunkMaxTokens;

    /** 相邻正文分片的重叠Token数。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("50")
    private Integer chunkOverlapTokens;

    /** 全文候选分片数量上限。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("20")
    private Integer keywordTopK;

    /** 向量候选分片数量上限。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("20")
    private Integer vectorTopK;

    /** 单份资料检索返回数量上限。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("10")
    private Integer retrievalTopK;

    /** 向量相似度阈值，取值0至1，可为空。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Double similarityThreshold;

    /** 原始文件名，仅作展示，不参与存储路径拼接。 */
    private String originalFilename;

    /** 原件存储类型，当前为minio。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("T(org.etd.ai.knowledge.constant.KnowledgePolicy).MINIO")
    private String storageProvider;

    /** 原件所在的私有存储桶。 */
    private String storageBucket;

    /** 后端生成的对象键，不保存临时签名URL。 */
    private String storageObjectKey;

    /** 客户端声明的MIME类型，不表示解析器已支持该格式。 */
    private String contentType;

    /** 声明的文件总大小，单位字节，合并后必须核验。 */
    private Long fileSize;

    /** 客户端计算的原始文件SHA-256，合并后由服务端核验。 */
    private String fileSha256;

    /** MinIO分片上传会话ID，不允许客户端指定。 */
    private String uploadId;

    /** 文件传输状态，与知识解析状态独立。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("T(org.etd.ai.knowledge.constant.KnowledgePolicy).INITIALIZING")
    private String uploadStatus;

    /** 固定上传分片大小，单位字节，末片可以较小。 */
    private Long partSizeBytes;

    /** 上传分片总数，不超过10000。 */
    private Integer partCount;

    /** 后端核实的上传分片清单，JSON持久化。 */
    @TableField(typeHandler = UploadPartsTypeHandler.class)
    private List<UploadPart> uploadParts;

    /** 允许恢复上传的截止时间。 */
    private Instant uploadExpiresAt;

    /** 完整原件核验通过时间。 */
    private Instant uploadedTime;

    /** 脱敏上传失败原因，不包含签名地址。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String uploadErrorMessage;

    /** 知识处理状态：uploaded、pending、parsing、preview、embedding、ready、failed。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("T(org.etd.ai.knowledge.constant.KnowledgePolicy).UPLOADED")
    private String processingStatus;

    /** 处理轮次，重新处理递增，旧结果不得参与检索。 */
    @TableField(fill = FieldFill.INSERT)
    @TableFieldExtend("0")
    private Long processingVersion;

    /** 当前处理任务的唯一租约ID。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS, typeHandler = ProcessingOwnerTypeHandler.class)
    private UUID processingOwner;

    /** 处理租约到期时间，超时任务可被新轮次接管。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Instant processingLeaseUntil;

    /** 本轮处理开始时间。 */
    private Instant processingStartedTime;

    /** 本轮采用的解析器版本。 */
    private String parserVersion;

    /** 本轮采用的正文分片规则版本。 */
    private String splitterVersion;

    /** 脱敏处理失败摘要。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String errorMessage;

    /** 本轮处理结束时间。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Instant processedTime;

}
