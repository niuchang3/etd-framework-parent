package org.etd.framework.business.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.etd.framework.starter.mybaits.core.BaseEntity;

import java.io.Serializable;

@Data
@TableName("system_menus")
public class SystemMenusEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = -4312600779837990536L;

    @TableField("PARENT_ID")
    private Long parentId;

    @TableField("MENU_NAME")
    private String menuName;

    @TableField("MENU_ROUTER")
    private String menuRouter;

    @TableField("MENU_ICON")
    private String menuIcon;

    @TableField("SORT")
    private Integer sort;

}
