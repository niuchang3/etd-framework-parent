package org.etd.framework.starter.job.process;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.starter.job.handler.FailureHandler;
import org.etd.framework.starter.job.handler.SuccessHandler;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 任务处理器基类。
 * <p>
 * 管理并触发任务执行成功与失败的回调处理程序链。
 *
 * @param <C> 任务执行上下文类型
 */
@Slf4j
public abstract class AbstractJobProcessor<C> {

    /**
     * 执行成功处理程序
     */
    private List<SuccessHandler<C>> successHandlers;
    /**
     * 执行失败处理程序
     */
    private List<FailureHandler<C>> failureHandlers;


    /**
     * 添加 Success Handler
     *
     * @param handler 参数 handler
     */
    public void addSuccessHandler(SuccessHandler<C> handler) {
        if (CollectionUtils.isEmpty(successHandlers)) {
            successHandlers = Lists.newArrayList();
        }
        successHandlers.add(handler);
    }

    /**
     * 添加 Failure Handler
     *
     * @param handler 参数 handler
     */
    public void addFailureHandler(FailureHandler<C> handler) {
        if (CollectionUtils.isEmpty(failureHandlers)) {
            failureHandlers = Lists.newArrayList();
        }
        failureHandlers.add(handler);
    }

    /**
     * 顺序触发注册的所有成功回调处理程序。
     *
     * @param c 任务上下文
     */
    protected void invokeSuccessHandlers(C c) {
        if (CollectionUtils.isEmpty(successHandlers)) {
            return;
        }
        for (var successHandler : successHandlers) {
            try {
                successHandler.successExecute(c);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 顺序触发注册的所有失败回调处理程序。
     *
     * @param c         任务上下文
     * @param exception 捕获到的异常实例
     */
    /**
     * invoke Failure Handlers
     *
     * @param c 参数 c
     * @param exception 参数 exception
     */
    protected void invokeFailureHandlers(C c, Exception exception) {
        if (CollectionUtils.isEmpty(failureHandlers)) {
            return;
        }
        for (var failureHandler : failureHandlers) {
            try {
                failureHandler.failedExecute(c, exception);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
