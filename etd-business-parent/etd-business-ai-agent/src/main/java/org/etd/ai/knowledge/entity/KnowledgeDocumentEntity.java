package org.etd.ai.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import org.etd.ai.knowledge.mapper.typehandler.UploadPartsTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.ai.knowledge.model.UploadPart;
import org.etd.framework.starter.mybaits.core.OrgScopedEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;
import java.time.Instant;
import java.util.List;

/** 资料及上传主记录，仅维护基本信息、原件和上传进度。 */
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

    /** 原始文件名，仅作展示，不参与存储路径拼接。 */
    private String originalFilename;

    /** 原始文件扩展名，统一小写且不含点；无扩展名为unknown，仅供图标展示，不代表内容检测结果。 */
    private String fileType;

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

    /** 文件传输状态，用于展示原件上传进度。 */
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

}
