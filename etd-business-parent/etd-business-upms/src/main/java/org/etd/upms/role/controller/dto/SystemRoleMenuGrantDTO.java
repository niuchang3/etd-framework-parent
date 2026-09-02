package org.etd.upms.role.controller.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.etd.framework.common.core.constants.BasicConstant;

import java.util.Arrays;

/**
 * 角色菜单授权明细 DTO。
 */
@Data
public class SystemRoleMenuGrantDTO {

    @NotNull(message = "菜单ID不能为空")
    private Long menuId;

    @NotNull(message = "菜单访问级别不能为空")
    private String accessLevel;

    /**
     * 判断 AccessLevelValid 状态
     *
     * @return 处理结果
     */
    @AssertTrue(message = "菜单访问级别只能为 READ_ONLY 或 READ_WRITE")
    public boolean isAccessLevelValid() {
        return accessLevel != null && Arrays.stream(BasicConstant.AccessLevel.values())
                .anyMatch(level -> level.getCode().equals(accessLevel));
    }
}
