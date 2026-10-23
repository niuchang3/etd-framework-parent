package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.etd.ai.knowledge.constant.KnowledgePolicy;

/** 创建上传记录的请求；扩展名参数仅供前端筛选，后端不按扩展名拒绝上传。 */
@Data
public class KnowledgeUploadInitDTO {
    /** 是否在当前租户内共享读取，false仍按角色数据范围可见。 */
    @NotNull
    private Boolean isPublic;

    /** 上传目标目录ID，后端校验维护权限。 */
    @NotNull @Positive
    private Long folderId;
    /** 资料数据归属部门，须在当前用户可维护范围中。 */
    @NotNull @Positive
    private Long orgId;
    /** 原始文件名，不作为对象键。 */
    @NotBlank @Size(max = 255)
    private String originalFilename;
    /** 文件MIME声明，空时前端可传application/octet-stream。 */
    @NotBlank @Size(max = 255)
    private String contentType;
    /** 文件总字节数。 */
    @NotNull @Min(1)
    private Long fileSize;
    /** 文件内容SHA-256，用于续传身份识别与最终完整性核验。 */
    @NotBlank @Pattern(regexp = KnowledgePolicy.SHA256_PATTERN)
    private String fileSha256;
}
