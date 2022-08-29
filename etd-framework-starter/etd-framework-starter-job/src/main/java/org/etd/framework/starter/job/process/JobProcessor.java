package org.etd.framework.starter.job.process;


public interface JobProcessor<C> {

    /**
     * 执行Job任务
     *
     * @param context
     */
    void doProcess(C context);
}
