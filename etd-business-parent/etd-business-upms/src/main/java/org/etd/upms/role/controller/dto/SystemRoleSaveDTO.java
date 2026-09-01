package org.etd.upms.role.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.etd.framework.common.core.constants.BasicConstant;

import java.util.Arrays;

@Data
public class SystemRoleSaveDTO {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 120, message = "角色名称不能超过120个字符")
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 50, message = "角色编码不能超过50个字符")
    private String roleCode;

    @Size(max = 200, message = "角色描述不能超过200个字符")
    private String roleDesc;

    @NotBlank(message = "数据权限类型不能为空")
    private String permissionType;

    @AssertTrue(message = "数据权限类型只能为1至5")
    public boolean isPermissionTypeValid() {
        return permissionType != null && Arrays.stream(BasicConstant.PermissionType.values())
                .anyMatch(type -> type.getCode().equals(permissionType));
    }
}
