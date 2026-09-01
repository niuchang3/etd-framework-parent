package org.etd.upms.tenant.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SystemTenantUpdateDTO {

    @Size(max = 200, message = "Logo地址不能超过200个字符")
    private String logo;

    @NotBlank(message = "租户名称不能为空")
    @Size(max = 100, message = "租户名称不能超过100个字符")
    private String tenantName;

    @Size(max = 200, message = "租户描述不能超过200个字符")
    private String description;

    @Size(max = 100, message = "统一社会信用代码不能超过100个字符")
    private String creditCode;

    @Size(max = 100, message = "租户类型不能超过100个字符")
    private String tenantType;
}
