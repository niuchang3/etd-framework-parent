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
public class Job<T extends Serializable> implements Serializable {

    /**
     * 任务ID
     */
    private String jobId;
    /**
     * 任务分组
     */
    private String jobGroup;
    /**
     * 任务类型，local，Feign调用，OR 其他
     */
    private String jobType;
    /**
     * 执行器
     */
    private String jobExecution;
    /**
     * 触发时间 cron 表达式
     */
    private String cronExpression;
    /**
     * 请求参数
     */
    private T body;
    /**
     * 默认执行一次
     */
    private Integer retry = 1;

    /**
     * 上下文信息
     */
    private Map<String, Object> context;


}
