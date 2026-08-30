package org.etd.upms.menu.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName(value = "sys_menus", excludeProperty = "tenantId")
public class SystemMenusEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -4312600779837990536L;

    @TableField("parent_id")
    private Long parentId;

    @TableField("menu_name")
    private String menuName;

    @TableField("menu_path")
    private String menuPath;

    @TableField("menu_router")
    private String menuRouter;

    @TableField("menu_icon")
    private String menuIcon;

    @TableField("menu_type")
    private String menuType;

    @TableField("sort")
    private Integer sort;

}
