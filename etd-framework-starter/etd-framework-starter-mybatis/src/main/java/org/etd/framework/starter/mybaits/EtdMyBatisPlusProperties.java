package org.etd.framework.starter.mybaits;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "etd.mybatis")
@Data
public class EtdMyBatisPlusProperties {
    /**
     * 租户相关配置
     */
    @NestedConfigurationProperty
    private Tenant tenant;
    /**
     * 数据快照开关
     */
    @NestedConfigurationProperty
    private Snapshot snapshot;

    /**
     * 数据填充开关
     */
    @NestedConfigurationProperty
    private Fill fill;

    /**
     * 数据权限
     */
    @NestedConfigurationProperty
    private Permission permission;

    /**
     * 数据快照配置
     */
    @Data
    public class Snapshot {

        private Boolean enabled = false;

    }

    @Data
    public class Fill {

        private Boolean enabled = false;
    }


    @Data
    public class Permission {
        /**
         * 全局性配置
         */
        private Boolean enabled = true;
        /**
         * 全局化配置，数据库表，表示部门ID的字段名称
         */
        private String globalDeptColumnName = "DEPT_ID";
        /**
         * 全局化配置， 数据库表，表示用户ID的字段名称
         */
        private String globalUserColumnName = "USER_ID";

//        @NestedConfigurationProperty
//        private List<Tables> tables = Lists.newArrayList();

    }

    @Data
    public class Tenant {

        private Boolean enabled = false;

        private String columnName = "TENANT_ID";
    }

}
