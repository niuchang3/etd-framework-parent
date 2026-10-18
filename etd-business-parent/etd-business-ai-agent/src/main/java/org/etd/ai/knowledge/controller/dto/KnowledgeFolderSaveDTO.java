package org.etd.ai.knowledge.controller.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

import org.etd.framework.starter.web.validation.UpdateRequest;

/** KnowledgeFolderSaveDTO 页面请求；不接收租户、创建人或存储对象键。 */
@Data
public class KnowledgeFolderSaveDTO {
    /** 父目录，根目录为空。 */
    @Positive private Long parentId;

    /** 目录名称。 */
    @NotBlank
    @Size(max = 100)
    private String folderName;

    /** 同级非负排序。 */
    @NotNull
    @Min(0)
    private Integer sortOrder;

    /** 是否公开读取，新增默认false。 */
    private Boolean isPublic;

    /** 更新必填的版本。 */
    @Min(0)
    @NotNull(groups = UpdateRequest.class)
    private Long version;
}
