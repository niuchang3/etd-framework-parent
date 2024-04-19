package org.etd.framework.common.core.constants;

public interface BasicConstant {


    enum  MessageHeader {
        /**
         * 租户ID
         */
        tenant_id;
    }

    /**
     * 租户类型
     */
    enum TenantType {
        System,
        Ordinary
    }

    /**
     * 系统内置角色
     */
    enum SystemRole {
        //普通用户
        Ordinary,
        //租户管理员
        TenantAdmin,
        // 平台管理员
        PlatformAdmin;
    }

    enum PermissionType{
        // 所有数据权限
        ALL,
        //用户级数据权限
        USER,
        // 部门级数据权限
        Department,
        // 部门及下级部门数据权限
        DepartmentAndSubordinate
    }
}
