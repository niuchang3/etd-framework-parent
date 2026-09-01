package org.etd.upms.tenant.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class SystemTenantMenuAssignDTO {

    @NotNull(message = "菜单ID集合不能为空")
    private Set<Long> menuIds;
}
