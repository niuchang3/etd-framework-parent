package org.etd.framework.common.core.constants;

public interface BasicConstant {

    /**
     * 全局数据状态
     */
    enum DataStatus {
        /**
         * 禁用
         */
        DISABLED(0),
        /**
         * 启用
         */
        ENABLED(1);

        public static final int DISABLED_CODE = 0;
        public static final int ENABLED_CODE = 1;

        private final int code;

        DataStatus(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * 租户类型
     */
    enum TenantType {
        SYSTEM("system"),
        ORDINARY("ordinary");

        private final String code;

        TenantType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * 系统内置角色
     */
    enum SystemRole {
        /**
         * 普通用户
         */
        ORDINARY("ordinary"),
        /**
         * 租户管理员
         */
        TENANT_ADMIN("tenantadmin"),
        /**
         * 平台管理员
         */
        PLATFORM_ADMIN("platformadmin");

        private final String code;

        SystemRole(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    enum PermissionType {
        /**
         * 所有数据权限
         */
        ALL("all"),
        /**
         * 用户级数据权限
         */
        USER("user"),
        /**
         * 部门级数据权限
         */
        DEPARTMENT("department"),
        /**
         * 部门及下级部门数据权限
         */
        DEPARTMENT_AND_SUBORDINATE("department_and_subordinate");

        private final String code;

        PermissionType(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
