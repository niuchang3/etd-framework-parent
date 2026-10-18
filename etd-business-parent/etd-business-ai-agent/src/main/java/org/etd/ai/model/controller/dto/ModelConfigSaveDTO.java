package org.etd.ai.model.controller.dto;

import jakarta.validation.constraints.*;

import lombok.Data;

import org.etd.ai.model.constant.ModelProtocol;
import org.etd.framework.starter.web.validation.UpdateRequest;

/** ModelConfigSaveDTO 页面请求；不接收租户、创建人或存储对象键。 */
@Data
public class ModelConfigSaveDTO {
    /** 本租户连接标识。 */
    @NotNull @Positive private Long connectionId;

    /** 显示名称。 */
    @NotBlank
    @Size(max = 100)
    private String modelName;

    /** 主能力类型。 */
    @NotBlank
    @Pattern(regexp = ModelProtocol.TYPE_PATTERN)
    private String modelType;

    /** 真实模型代码。 */
    @NotBlank
    @Size(max = 200)
    private String modelCode;

    /** 用途说明。 */
    @Size(max = 500)
    private String remark;

    /** 更新时的版本。 */
    @Min(0)
    @NotNull(groups = UpdateRequest.class)
    private Long version;
}
