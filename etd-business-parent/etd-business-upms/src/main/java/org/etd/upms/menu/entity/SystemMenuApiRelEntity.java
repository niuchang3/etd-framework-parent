package org.etd.upms.menu.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

/**
 * 菜单与 API 接口权限关联关系实体类。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "sys_menu_api_rel", excludeProperty = "tenantId")
public class SystemMenuApiRelEntity extends BaseEntity {

    @TableField("menu_id")
    private Long menuId;

    @TableField("api_id")
    private Long apiId;
}
