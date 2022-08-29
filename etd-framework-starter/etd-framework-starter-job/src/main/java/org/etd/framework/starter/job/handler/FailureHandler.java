package org.etd.framework.starter.job.handler;

/**
 * 程序执行失败处理程序
 *
 * @param <T>
 */
public interface FailureHandler<T> {

    /**
     * 执行失败
     *
     * @param t
     */
    void failedExecute(T t, Exception exception);
}
