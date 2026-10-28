package org.etd.framework.starter.mybaits;

import com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.etd.framework.starter.mybaits.permission.handler.EtdDataPermissionInterceptor;
import org.etd.framework.starter.mybaits.logic.EtdLogicDeleteInterceptor;
import org.etd.framework.starter.mybaits.permission.aspect.DataPermissionAspect;
import org.etd.framework.starter.mybaits.permission.handler.EtdDataPermissionHandler;
import org.etd.framework.starter.mybaits.tenant.EtdTenantLineHandler;
import org.etd.framework.starter.mybaits.tenant.aspect.IgnoreTenantAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Young  经验
 * @description
 * @date 2020/6/23
 */

@Configuration
@ComponentScan({"org.etd.framework.starter.mybaits.*", "com.baomidou.mybatisplus.core.mapper"})
@EnableConfigurationProperties(value = EtdMyBatisPlusProperties.class)
public class MybatisConfig {

    private final EtdMyBatisPlusProperties properties;

    public MybatisConfig(EtdMyBatisPlusProperties properties) {
        this.properties = properties;
    }

    /**
     * MybatisPlus 拦截器配置
     *
     * @return
     */
    /**
     * mybatis Plus Interceptor
     *
     * @return 处理结果
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if(properties.getTenant().getEnabled()){
            TenantLineInnerInterceptor tenantLineInnerInterceptor = new TenantLineInnerInterceptor();
            EtdTenantLineHandler etdTenantLineHandler = new EtdTenantLineHandler(properties.getTenant().getColumnName());
            etdTenantLineHandler.addIgnoreTables(properties.getTenant().getIgnoreTables());
            tenantLineInnerInterceptor.setTenantLineHandler(etdTenantLineHandler);
            interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
        }

        if (Boolean.TRUE.equals(properties.getDataPermission().getEnabled())) {
            EtdDataPermissionHandler dataPermissionHandler = new EtdDataPermissionHandler(properties.getDataPermission());
            EtdDataPermissionInterceptor dataPermissionInterceptor = new EtdDataPermissionInterceptor(dataPermissionHandler);
            interceptor.addInnerInterceptor(dataPermissionInterceptor);
        }

        // 与组织数据权限独立执行，确保公开读取、忽略组织权限时仍排除已删除资料。
        if (Boolean.TRUE.equals(properties.getLogicDelete().getEnabled())) {
            EtdLogicDeleteInterceptor logicDeleteInterceptor = new EtdLogicDeleteInterceptor();
            interceptor.addInnerInterceptor(logicDeleteInterceptor);
        }

        // 仅对显式声明@Version的实体生效；缺少版本时拒绝更新，禁止静默跳过并发保护。
        OptimisticLockerInnerInterceptor optimisticLocker = new OptimisticLockerInnerInterceptor();
        IllegalArgumentException missingVersionException = new IllegalArgumentException("更新实体必须提供version");
        optimisticLocker.setException(missingVersionException);
        interceptor.addInnerInterceptor(optimisticLocker);
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        interceptor.addInnerInterceptor(paginationInterceptor);
        BlockAttackInnerInterceptor blockAttackInterceptor = new BlockAttackInnerInterceptor();
        interceptor.addInnerInterceptor(blockAttackInterceptor);

        return interceptor;
    }


    /**
     * ignore Tenant Aspect
     *
     * @return 处理结果
     */
    @ConditionalOnProperty(prefix = "etd.mybatis.tenant", value = "enabled",matchIfMissing = true)
    @Bean
    public IgnoreTenantAspect ignoreTenantAspect(){
        return new IgnoreTenantAspect();
    }

    /**
     * data Permission Aspect
     *
     * @return 处理结果
     */
    @ConditionalOnProperty(prefix = "etd.mybatis.data-permission", name = "enabled", havingValue = "true")
    @Bean
    public DataPermissionAspect dataPermissionAspect() {
        return new DataPermissionAspect();
    }


    /**
     * identifier Generator
     *
     * @param properties 参数 properties
     * @return 处理结果
     */
    @Bean
    @ConditionalOnProperty(prefix = "etd.mybatis.idGenerator", value = "enabled",matchIfMissing = true)
    public IdentifierGenerator identifierGenerator(EtdMyBatisPlusProperties properties) {
        EtdMyBatisPlusProperties.IdGeneratorProperties idGenerator = properties.getIdGenerator();
        return new DefaultIdentifierGenerator(
                idGenerator.getWorkerId(),
                idGenerator.getDatacenterId()
        );
    }
}
