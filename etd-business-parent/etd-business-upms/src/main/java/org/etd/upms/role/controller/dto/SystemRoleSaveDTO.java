package org.etd.upms.role.controller.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.etd.framework.common.core.constants.BasicConstant;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 角色保存与更新 DTO。
 */
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

    @NotNull(message = "自定义数据权限组织ID集合不能为空")
    private Set<@NotNull(message = "组织ID不能为空") @Positive(message = "组织ID必须大于0") Long>
            organizationIds = new LinkedHashSet<>();

    /**
     * 判断 PermissionTypeValid 状态
     *
     * @return 处理结果
     */
    @AssertTrue(message = "数据权限类型只能为1至5")
    public boolean isPermissionTypeValid() {
        return permissionType != null && Arrays.stream(BasicConstant.PermissionType.values())
                .anyMatch(type -> type.getCode().equals(permissionType));
    }

    /**
     * 判断 CustomOrganizationSelectionValid 状态
     *
     * @return 处理结果
     */
    @AssertTrue(message = "自定义跨组织数据权限至少需要选择一个组织")
    public boolean isCustomOrganizationSelectionValid() {
        return !BasicConstant.PermissionType.CUSTOM_ORGANIZATION.getCode().equals(permissionType)
                || organizationIds == null
                || !organizationIds.isEmpty();
    }
}
