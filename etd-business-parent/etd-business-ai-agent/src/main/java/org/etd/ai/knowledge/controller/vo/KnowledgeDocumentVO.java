package org.etd.ai.knowledge.controller.vo;

import lombok.Data;
import org.etd.ai.knowledge.model.UploadPart;
import java.time.Instant;
import java.util.List;

/** 资料管理响应，省略原件位置和对象存储会话标识。 */
@Data
public class KnowledgeDocumentVO {
    /** 是否在当前租户内共享读取；分片继承所属资料。 */
    private Boolean isPublic;
    /** 当前角色数据范围是否允许维护；还须具备接口write权限。 */
    private Boolean writable;
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

    /** 原始文件名，仅作展示，不参与存储路径拼接。 */
    private String originalFilename;

    /** 原始文件扩展名，统一小写且不含点；无扩展名为unknown，仅供图标展示，不代表内容检测结果。 */
    private String fileType;

    /** 客户端声明的MIME类型，不表示解析器已支持该格式。 */
    private String contentType;

    /** 声明的文件总大小，单位字节，合并后必须核验。 */
    private Long fileSize;

    /** 客户端计算的原始文件SHA-256，合并后由服务端核验。 */
    private String fileSha256;

    /** 文件传输状态，用于展示原件上传进度。 */
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

    /** 分片状态，与上传状态分开展示。 */
    private String chunkStatus;
    /** 已保存的分片数。 */
    private Integer chunkCount;
    /** 最近一次分片生成的脱敏错误摘要。 */
    private String chunkErrorMessage;
}
