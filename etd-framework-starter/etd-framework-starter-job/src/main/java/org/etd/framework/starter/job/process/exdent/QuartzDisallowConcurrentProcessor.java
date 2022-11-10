package org.etd.framework.starter.job.process.exdent;

import org.etd.framework.starter.job.process.AbstractQuartzJobProcessor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;

/**
 * 不允许并发处理器
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentProcessor extends AbstractQuartzJobProcessor {


    @Override
    public void doProcess(JobExecutionContext context) {

    }
}
