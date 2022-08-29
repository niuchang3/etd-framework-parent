package org.etd.framework.starter.job.process;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.starter.job.handler.FailureHandler;
import org.etd.framework.starter.job.handler.SuccessHandler;
import org.springframework.util.CollectionUtils;

import java.util.List;

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


    public void addSuccessHandler(SuccessHandler<C> handler) {
        if (CollectionUtils.isEmpty(successHandlers)) {
            successHandlers = Lists.newArrayList();
        }
        successHandlers.add(handler);
    }

    public void addFailureHandler(FailureHandler<C> handler) {
        if (CollectionUtils.isEmpty(failureHandlers)) {
            failureHandlers = Lists.newArrayList();
        }
        failureHandlers.add(handler);
    }


    protected void invokeSuccessHandlers(C c) {
        if (CollectionUtils.isEmpty(successHandlers)) {
            return;
        }
        successHandlers.stream().forEach(failureHandler -> {
            try {
                failureHandler.successExecute(c);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    protected void invokeFailureHandlers(C c, Exception exception) {
        if (CollectionUtils.isEmpty(failureHandlers)) {
            return;
        }
        failureHandlers.stream().forEach(failureHandler -> {
            try {
                failureHandler.failedExecute(c, exception);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        });
    }
}
