package org.etd.ai.document.controller.vo;

import lombok.Data;

import java.time.Instant;

/** KnowledgeDocumentVO 页面响应；所有业务ID按框架Long序列化约定传输。 */
@Data
public class KnowledgeDocumentVO {
    /** 雪花业务标识。 */
    private Long id;

    /** 启停状态：0停用、1启用，对应全局BasicConstant.DataStatus。 */
    private Integer dataStatus;

    /** 更新需要回传的乐观锁版本。 */
    private Long version;

    /** 创建时间点。 */
    private Instant createTime;

    /** 更新时间点。 */
    private Instant updateTime;

    /** 知识库ID。 */
    private Long knowledgeBaseId;

    /** 目录ID，空表示未分类。 */
    private Long folderId;

    /** 文件归属组织。 */
    private Long orgId;

    /** 创建用户ID。 */
    private Long createBy;

    /** 文件显示名称。 */
    private String documentName;

    /** 原始文件名。 */
    private String originalFilename;

    /** 后端按文件名推断的MIME类型，不代表已经校验格式。 */
    private String contentType;

    /** 原件字节数。 */
    private Long fileSize;

    /** 后端计算的摘要。 */
    private String fileSha256;

    /** 是否租户内公开。 */
    private Boolean isPublic;

    /** 本阶段新文件均为uploaded。 */
    private String processingStatus;

    /** 内容处理轮次。 */
    private Long processingVersion;

    /** 框架数据范围是否允许管理；操作同时要求接口write权限。 */
    private Boolean writable;
}
