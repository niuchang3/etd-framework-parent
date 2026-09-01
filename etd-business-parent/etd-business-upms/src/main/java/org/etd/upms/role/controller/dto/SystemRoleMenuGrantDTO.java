package org.etd.upms.role.controller.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.etd.framework.common.core.constants.BasicConstant;

import java.util.Arrays;

@Data
public class SystemRoleMenuGrantDTO {

    @NotNull(message = "菜单ID不能为空")
    private Long menuId;

    @NotNull(message = "菜单访问级别不能为空")
    private String accessLevel;

    @AssertTrue(message = "菜单访问级别只能为 READ_ONLY 或 READ_WRITE")
    public boolean isAccessLevelValid() {
        return accessLevel != null && Arrays.stream(BasicConstant.AccessLevel.values())
                .anyMatch(level -> level.getCode().equals(accessLevel));
    }
}
