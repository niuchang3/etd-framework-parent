package org.etd.framework.starter.job.process;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz Job 处理器
 */
@Slf4j
public abstract class AbstractQuartzJobProcessor extends AbstractJobProcessor<JobExecutionContext> implements JobProcessor<JobExecutionContext>, Job {


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
//            context.get()
            JobDataMap mergedJobDataMap = context.getMergedJobDataMap();
//            mergedJobDataMap
            doProcess(context);
            invokeSuccessHandlers(context);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            invokeFailureHandlers(context, e);
        }
    }


    @Override
    public void doProcess(JobExecutionContext context) {

    }
}
