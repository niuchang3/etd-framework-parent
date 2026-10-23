package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** 资料配置编辑请求，不允许修改文件会话、归属部门及可信审计字段。 */
@Data
public class KnowledgeDocumentSaveDTO {
    /** 是否在当前租户内共享读取，false仍按角色数据范围可见。 */
    @NotNull
    private Boolean isPublic;

    /** 编辑所依据的乐观锁版本。 */
    @NotNull @Min(0)
    private Long version;

    /** 所属分类目录ID。 */
    @NotNull @Positive
    private Long folderId;

    /** 资料显示名称，最多255字。 */
    @NotBlank @Size(max = 255)
    private String documentName;

    /** 资料用途说明，最多1000字，可清空。 */
    @Size(max = 1000)
    private String description;

}
