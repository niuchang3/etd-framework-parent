package org.etd.upms.organization.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_organization")
public class SystemOrganizationEntity extends BaseEntity {

    @TableField("parent_id")
    private Long parentId;

    @TableField("parent_id_path")
    private String parentIdPath;

    @TableField("org_code")
    private String orgCode;

    @TableField("org_name")
    private String orgName;

    @TableField("org_type")
    private String orgType;

    @TableField("leader_user_id")
    private Long leaderUserId;

    @TableField("sort")
    private Integer sort;

    @TableField("enabled")
    private Boolean enabled;
}
