package org.etd.framework.starter.job.process.exdent;

import org.etd.framework.starter.job.process.AbstractQuartzJobProcessor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

/**
 * 不允许并发处理器
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentProcessor extends AbstractQuartzJobProcessor {


    /**
     * do 处理执行
     *
     * @param context 参数 context
     */
    @Override
    public void doProcess(JobExecutionContext context) {

    }
}
