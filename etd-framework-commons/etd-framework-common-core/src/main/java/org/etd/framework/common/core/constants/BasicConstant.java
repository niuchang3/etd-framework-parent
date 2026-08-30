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
        TENANT_ADMIN("tenantAdmin"),
        /**
         * 平台管理员
         */
        PLATFORM_ADMIN("platformAdmin");

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
         * 不限制数据权限
         */
        ALL("1", "不限制数据权限"),
        /**
         * 仅本人数据权限
         */
        SELF("2", "仅本人数据权限"),
        /**
         * 仅当前组织数据权限
         */
        ORGANIZATION("3", "仅组织数据权限"),
        /**
         * 当前组织及下级组织数据权限
         */
        ORGANIZATION_AND_SUBORDINATE("4", "组织及下级组织数据权限"),
        /**
         * 自定义跨组织数据权限
         */
        CUSTOM_ORGANIZATION("5", "自定义跨组织数据权限");

        private final String code;
        private final String description;

        PermissionType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 功能访问级别
     */
    enum AccessLevel {
        /**
         * 只读
         */
        READ(1, "只读"),
        /**
         * 读写
         */
        WRITE(2, "读写");

        private final int code;
        private final String description;

        AccessLevel(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}
