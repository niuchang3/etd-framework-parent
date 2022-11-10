package org.etd.framework.starter.job.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 任务
 *
 * @param <T>
 */
@Data
public class DefaultJob implements Serializable {

    /**
     * 任务ID
     */
    private String jobId;
    /**
     * 任务分组
     */
    private String jobGroup;
    /**
     * 执行器
     */
    private String jobExecution;
    /**
     * 触发时间 cron 表达式
     */
    private String cronExpression;
    /**
     * Job状态
     */
    private String status;
    /**
     * 任务执行次数
     */
    private Integer executedCount;
    /**
     * 默认执行一次
     */
    private Integer retry = 1;

    /**
     * 上下文信息
     */
    private Map<String, Object> context;


}
