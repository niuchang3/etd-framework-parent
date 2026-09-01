package org.etd.upms.tenant.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SystemTenantAdminCreateDTO {

    @NotBlank(message = "管理员账号不能为空")
    @Size(max = 32, message = "管理员账号不能超过32个字符")
    private String account;

    @NotBlank(message = "管理员密码不能为空")
    @Size(min = 8, max = 72, message = "管理员密码长度必须在8至72个字符之间")
    private String password;

    @NotBlank(message = "管理员姓名不能为空")
    @Size(max = 20, message = "管理员姓名不能超过20个字符")
    private String userName;

    @Size(max = 20, message = "管理员手机号不能超过20个字符")
    private String mobile;
}
