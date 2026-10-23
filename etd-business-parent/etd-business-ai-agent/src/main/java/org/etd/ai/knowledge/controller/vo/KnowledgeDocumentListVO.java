package org.etd.ai.knowledge.controller.vo;

import lombok.Data;
import java.time.Instant;

/** 资料列表摘要；上传分片清单只在详情返回，避免分页传输大量回执。 */
@Data
public class KnowledgeDocumentListVO {
    /** 是否在当前租户内共享读取；分片继承所属资料。 */
    private Boolean isPublic;
    /** 当前角色数据范围是否允许维护；还须具备接口write权限。 */
    private Boolean writable;
    /** 资料ID。 */
    private Long id;
    /** 资料的数据归属部门ID。 */
    private Long orgId;
    /** 所属分类目录ID。 */
    private Long folderId;
    /** 编辑时使用的乐观锁版本。 */
    private Long version;
    /** 资料显示名称。 */
    private String documentName;
    /** 原始文件名。 */
    private String originalFilename;

    /** 原始文件扩展名，统一小写且不含点；无扩展名为unknown，仅供图标展示，不代表内容检测结果。 */
    private String fileType;
    /** 文件MIME声明。 */
    private String contentType;
    /** 原件大小，单位字节。 */
    private Long fileSize;
    /** 资料启停状态。 */
    private Integer dataStatus;
    /** 文件上传状态：initializing、uploading、completing、completed、failed、cancelled。 */
    private String uploadStatus;

    /** 脱敏后的上传失败原因。 */
    private String uploadErrorMessage;

    /** 上传会话截止时间。 */
    private Instant uploadExpiresAt;
    /** 资料上传用户ID。 */
    private Long createBy;
    /** 创建时间。 */
    private Instant createTime;
    /** 修改时间。 */
    private Instant updateTime;
    /** 分片状态，与上传状态分开展示。 */
    private String chunkStatus;
    /** 已保存的分片数。 */
    private Integer chunkCount;
    /** 最近一次分片生成的脱敏错误摘要。 */
    private String chunkErrorMessage;
}
