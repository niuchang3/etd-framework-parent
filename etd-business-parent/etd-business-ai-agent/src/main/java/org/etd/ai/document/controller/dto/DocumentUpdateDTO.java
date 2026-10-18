package org.etd.ai.document.controller.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

/** DocumentUpdateDTO 页面请求；不接收租户、创建人或存储对象键。 */
@Data
public class DocumentUpdateDTO {
    /** 显示名称。 */
    @NotBlank
    @Size(max = 255)
    private String documentName;

    /** 目标目录，空表示未分类。 */
    @Positive private Long folderId;

    /** 是否租户内公开。 */
    @NotNull private Boolean isPublic;

    /** 当前乐观锁版本。 */
    @NotNull
    @Min(0)
    private Long version;
}
