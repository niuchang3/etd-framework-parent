package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

import org.etd.framework.starter.web.validation.UpdateRequest;

/** KnowledgeFolderSaveDTO 页面请求；不接收租户、创建人或存储对象键。 */
@Data
public class KnowledgeFolderSaveDTO {
    /** 所属知识库ID。 */
    @NotNull
    @Positive
    private Long knowledgeBaseId;

    /** 父目录，根目录为空。 */
    @Positive private Long parentId;

    /** 目录名称。 */
    @NotBlank
    @Size(max = 100)
    private String folderName;

    /** 目录描述，选填，最多500字。 */
    @Size(max = 500)
    private String description;

    /** 同级非负排序。 */
    @NotNull
    @Min(0)
    private Integer sortOrder;

    /** 更新必填的版本。 */
    @Min(0)
    @NotNull(groups = UpdateRequest.class)
    private Long version;
}
