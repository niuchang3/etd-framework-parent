package org.etd.framework.starter.mybaits.snapshot;

import org.etd.framework.starter.mybaits.snapshot.aspect.DataSnapshotAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SnapshotAutoConfiguration {


    @ConditionalOnProperty(prefix = "etd.mybatis.snapshot", value = "enabled",matchIfMissing = true)
    @Bean
    public DataSnapshotAspect dataSnapshotAspect() {
        return new DataSnapshotAspect();
    }
}
