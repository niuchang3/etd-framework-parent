package org.etd.framework.starter.job.process.exdent;

import org.etd.framework.starter.job.process.AbstractQuartzJobProcessor;
import org.quartz.JobExecutionContext;

/**
 * 并发处理器
 */
public class QuartzConcurrentProcessor extends AbstractQuartzJobProcessor {

    /**
     * do 处理执行
     *
     * @param context 参数 context
     */
    @Override
    public void doProcess(JobExecutionContext context) {

    }
}
