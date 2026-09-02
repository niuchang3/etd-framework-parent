package org.etd.upms.role.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class SystemRoleOrganizationAssignDTO {

    @NotEmpty(message = "自定义跨组织数据权限至少需要选择一个组织")
    private Set<@NotNull(message = "组织ID不能为空") @Positive(message = "组织ID必须大于0") Long>
            organizationIds = new LinkedHashSet<>();
}
