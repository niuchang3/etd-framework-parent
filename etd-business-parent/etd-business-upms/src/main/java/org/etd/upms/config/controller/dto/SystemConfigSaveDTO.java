package org.etd.upms.config.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SystemConfigSaveDTO {

    @NotBlank(message = "参数键不能为空")
    @Size(max = 100, message = "参数键不能超过100个字符")
    private String parameterKey;

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 100, message = "参数名称不能超过100个字符")
    private String parameterName;

    private String parameterValue;

    @NotBlank(message = "参数值类型不能为空")
    @Size(max = 50, message = "参数值类型不能超过50个字符")
    private String valueType;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
