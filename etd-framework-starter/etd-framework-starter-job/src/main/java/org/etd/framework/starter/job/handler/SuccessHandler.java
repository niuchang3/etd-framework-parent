package org.etd.framework.starter.job.handler;

/**
 * 执行成功处理程序
 * @param <T>
 */
public interface SuccessHandler<T> {

    void successExecute(T t);
}
