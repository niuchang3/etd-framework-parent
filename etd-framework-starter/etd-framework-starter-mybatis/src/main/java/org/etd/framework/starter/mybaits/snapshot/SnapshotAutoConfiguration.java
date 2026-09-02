package org.etd.framework.starter.mybaits.snapshot;

import org.etd.framework.starter.mybaits.snapshot.aspect.DataSnapshotAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * 数据快照功能 Spring 自动配置类。
 */
@Configuration
public class SnapshotAutoConfiguration {


    /**
     * data Snapshot Aspect
     *
     * @return 处理结果
     */
    @ConditionalOnProperty(prefix = "etd.mybatis.snapshot", value = "enabled",matchIfMissing = true)
    @Bean
    public DataSnapshotAspect dataSnapshotAspect() {
        return new DataSnapshotAspect();
    }
}
