package org.etd.upms.tenant.controller.vo;

import lombok.Data;
import org.etd.upms.menu.controller.vo.SystemMenuVO;

import java.util.List;
import java.util.Set;

/**
 * 租户菜单设置视图对象 VO。
 */
@Data
public class SystemTenantMenuSettingsVO {

    private List<SystemMenuVO> menus;

    private Set<Long> selectedMenuIds;
}
