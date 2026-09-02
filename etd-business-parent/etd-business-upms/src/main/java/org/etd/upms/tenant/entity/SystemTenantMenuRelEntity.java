package org.etd.upms.tenant.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

/**
 * 租户与菜单关联关系实体类。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_tenant_menu_rel")
public class SystemTenantMenuRelEntity extends BaseEntity {

    @TableField("menu_id")
    private Long menuId;
}
