package org.etd.framework.starter.mybaits.permission;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.etd.framework.starter.mybaits.EtdMyBatisPlusProperties;
import org.etd.framework.starter.mybaits.permission.Interceptors.DataPermissionInterceptor;
import org.etd.framework.starter.mybaits.permission.constant.DataPermissionsType;
import org.etd.framework.starter.mybaits.permission.handler.DataPermissionHandler;
import org.etd.framework.starter.mybaits.permission.handler.DefaultDataPermissionHandler;
import org.etd.framework.starter.mybaits.permission.model.Tables;
import org.etd.framework.starter.mybaits.utils.MyBatisUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Order(-1)
@Configuration
public class PermissionAutoConfiguration {


    @ConditionalOnProperty(prefix = "etd.mybatis.permission", value = "enabled", matchIfMissing = true)
    @ConditionalOnMissingBean(DataPermissionApi.class)
    @Bean
    public DataPermissionApi dataPermissionApi() {
        return new DefaultDataPermissionApi();
    }

    @ConditionalOnProperty(prefix = "etd.mybatis.permission", value = "enabled", matchIfMissing = true)
    @ConditionalOnMissingBean(DataPermissionHandler.class)
    @Bean
    public DataPermissionHandler dataPermissionHandler(EtdMyBatisPlusProperties properties, DataPermissionApi dataPermissionApi) {
        DefaultDataPermissionHandler handler = new DefaultDataPermissionHandler(dataPermissionApi);
        for (Tables table : properties.getPermission().getTables()) {
            table.setTableColumnName(getTableColumnName(table, properties));
            handler.addDataPermissionTable(table);
        }
        return handler;
    }


    @Bean
    public DataPermissionInterceptor dataPermissionDatabaseInterceptor(MybatisPlusInterceptor interceptor, DataPermissionHandler handler) {
        DataPermissionInterceptor inner = new DataPermissionInterceptor(handler);
        MyBatisUtils.addInterceptor(interceptor, inner, 0);
        return inner;
    }

    private String getTableColumnName(Tables table, EtdMyBatisPlusProperties properties) {
        if (DataPermissionsType.USER.equals(table.getDataPermissionsType())) {
            return StringUtils.isEmpty(table.getTableColumnName()) ? properties.getPermission().getGlobalUserColumnName() : table.getTableColumnName();
        }
        return StringUtils.isEmpty(table.getTableColumnName()) ? properties.getPermission().getGlobalDeptColumnName() : table.getTableColumnName();
    }
}
