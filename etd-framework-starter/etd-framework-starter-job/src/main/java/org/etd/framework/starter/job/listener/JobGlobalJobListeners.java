package org.etd.framework.starter.job.listener;

import org.etd.framework.common.core.context.AbstractRequestContextInitialization;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import java.util.Map;

public class JobGlobalJobListeners extends AbstractRequestContextInitialization<JobExecutionContext> implements JobListener {


    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 待执行时执行
     *
     * @param context
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {

    }

    /**
     * 任务被否决是执行
     *
     * @param context
     */
    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {

    }

    /**
     * 任务执行完时执行
     *
     * @param context
     * @param jobException
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {

    }

    @Override
    protected String getHeaderValue(JobExecutionContext jobExecutionContext, String headerName) {
        return null;
    }

    @Override
    protected Map<String, Object> getAttribute(JobExecutionContext jobExecutionContext) {
        return null;
    }

    @Override
    protected String getRemoteIp(JobExecutionContext jobExecutionContext) {
        return null;
    }
}
