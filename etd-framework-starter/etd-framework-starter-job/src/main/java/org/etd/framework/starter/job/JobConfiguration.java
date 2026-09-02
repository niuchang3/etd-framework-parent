package org.etd.framework.starter.job;


import org.etd.framework.starter.job.repository.JobRepository;
import org.etd.framework.starter.job.repository.exdent.MemoryJobRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Job 任务模块 Spring 自动配置类。
 */
@Configuration
@ComponentScan("org.etd.framework.starter.job.*")
public class JobConfiguration {

    /**
     * job Repository
     *
     * @return 处理结果
     */
    @Bean
    @ConditionalOnMissingBean(JobRepository.class)
    public JobRepository jobRepository() {
        return new MemoryJobRepository();
    }
}
