package org.etd.ai.knowledge.controller.vo;

import lombok.Data;
import org.etd.ai.knowledge.model.UploadPart;
import java.time.Instant;
import java.util.List;

/** 资料管理响应，省略原件位置和处理任务内部字段。 */
@Data
public class KnowledgeDocumentVO {
    /** 资料ID。 */
    private Long id;

    /** 资料数据归属部门ID。 */
    private Long orgId;

    /** 编辑所需乐观锁版本。 */
    private Long version;

    /** 启停状态。 */
    private Integer dataStatus;

    /** 上传用户ID。 */
    private Long createBy;

    /** 创建时间。 */
    private Instant createTime;

    /** 修改时间。 */
    private Instant updateTime;

    /** 所属分类目录ID。 */
    private Long folderId;

    /** 资料显示名称，最多255字。 */
    private String documentName;

    /** 资料用途说明，最多1000字，可清空。 */
    private String description;

    /** 是否允许租户内公开检索，不授予维护权限。 */
    private Boolean isPublic;

    /** 检索方式：keyword、vector、hybrid。 */
    private String retrievalMode;

    /** 向量模型连接器ID，全文模式可不配置。 */
    private Long embeddingModelConnectionId;

    /** 平台向量模型ID，由后端根据连接器确定。 */
    private Long embeddingModelId;

    /** 向量维度，仅允许768、1024或1536。 */
    private Integer embedding;

    /** 单个正文分片的Token上限。 */
    private Integer chunkMaxTokens;

    /** 相邻正文分片的重叠Token数。 */
    private Integer chunkOverlapTokens;

    /** 全文候选分片数量上限。 */
    private Integer keywordTopK;

    /** 向量候选分片数量上限。 */
    private Integer vectorTopK;

    /** 单份资料检索返回数量上限。 */
    private Integer retrievalTopK;

    /** 向量相似度阈值，取值0至1，可为空。 */
    private Double similarityThreshold;

    /** 原始文件名，仅作展示，不参与存储路径拼接。 */
    private String originalFilename;

    /** 客户端声明的MIME类型，不表示解析器已支持该格式。 */
    private String contentType;

    /** 声明的文件总大小，单位字节，合并后必须核验。 */
    private Long fileSize;

    /** 客户端计算的原始文件SHA-256，合并后由服务端核验。 */
    private String fileSha256;

    /** 文件传输状态，与知识解析状态独立。 */
    private String uploadStatus;

    /** 固定上传分片大小，单位字节，末片可以较小。 */
    private Long partSizeBytes;

    /** 上传分片总数，不超过10000。 */
    private Integer partCount;

    /** 后端核实的上传分片清单，JSON持久化。 */
    private List<UploadPart> uploadParts;

    /** 允许恢复上传的截止时间。 */
    private Instant uploadExpiresAt;

    /** 完整原件核验通过时间。 */
    private Instant uploadedTime;

    /** 脱敏上传失败原因，不包含签名地址。 */
    private String uploadErrorMessage;

    /** 知识处理状态：uploaded、pending、parsing、preview、embedding、ready、failed。 */
    private String processingStatus;

    /** 处理轮次，重新处理递增，旧结果不得参与检索。 */
    private Long processingVersion;

    /** 本轮处理开始时间。 */
    private Instant processingStartedTime;

    /** 本轮采用的解析器版本。 */
    private String parserVersion;

    /** 本轮采用的正文分片规则版本。 */
    private String splitterVersion;

    /** 脱敏处理失败摘要。 */
    private String errorMessage;

    /** 本轮处理结束时间。 */
    private Instant processedTime;

}
