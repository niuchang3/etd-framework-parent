package org.etd.upms.user.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户分配组织关联关系 DTO。
 */
@Data
public class SystemUserOrganizationAssignDTO {

    @NotNull(message = "组织ID集合不能为空")
    private Set<Long> organizationIds = new LinkedHashSet<>();

    private Long primaryOrganizationId;
}
