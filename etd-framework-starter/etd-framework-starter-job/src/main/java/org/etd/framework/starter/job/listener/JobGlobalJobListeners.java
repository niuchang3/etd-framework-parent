package org.etd.framework.starter.job.listener;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

/**
 * Quartz 定时任务全局监听器
 *
 * @author Young
 */
public class JobGlobalJobListeners implements JobListener {

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 任务待执行时钩子
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {

    }

    /**
     * 任务被否决时钩子
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {

    }

    /**
     * 任务执行完成时钩子
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {

    }
}
