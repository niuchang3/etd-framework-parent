package org.etd.framework.starter.mybaits;

import com.google.common.collect.Lists;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;

@ConfigurationProperties(prefix = "etd.mybatis")
@Data
public class EtdMyBatisPlusProperties {

    /**
     * 雪花 ID 生成器
     */
    @NestedConfigurationProperty
    private IdGeneratorProperties idGenerator = new IdGeneratorProperties();
    /**
     * 租户相关配置
     */
    @NestedConfigurationProperty
    private Tenant tenant = new Tenant();
    /**
     * 数据快照开关
     */
    @NestedConfigurationProperty
    private Snapshot snapshot = new Snapshot();

    /**
     * 数据填充开关
     */
    @NestedConfigurationProperty
    private Fill fill = new Fill();

    /**
     * 数据权限
     */
    @NestedConfigurationProperty
    private Permission permission = new Permission();


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
        private String globalDeptColumnName = "dept_id";
        /**
         * 全局化配置， 数据库表，表示用户ID的字段名称
         */
        private String globalUserColumnName = "user_id";

//        @NestedConfigurationProperty
//        private List<Tables> tables = Lists.newArrayList();

    }

    @Data
    public class Tenant {

        private Boolean enabled = false;

        private String columnName = "tenant_id";

        private List<String> ignoreTables = Lists.newArrayList();
    }


    @Data
    public class IdGeneratorProperties{
        /**
         * 是否启用
         */
        private Boolean enabled = false;
        /**
         * 机器 ID
         */
        private Integer workerId = 1;
        /**
         * 数据中心 ID
         */
        private Integer datacenterId =1;
    }

}
