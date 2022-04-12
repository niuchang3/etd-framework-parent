package org.etd.framework.common.core.listener;

import org.etd.framework.common.core.model.NotificationMsgRequest;
import org.etd.framework.common.utils.retry.RetryUtil;

import java.io.IOException;

public abstract class AbstractListenerHandler<M extends NotificationMsgRequest> implements ListenerHandler<M> {


    /**
     * 预留给子类的扩展钩子函数
     * 调用invokeProcessHandler之前执行该函数
     *
     * @param message
     */
    protected void beforeInvoke(M message) {

    }

    protected Object doProcessHandler(RetryUtil.Executor executor, M message) throws Exception {
        RetryUtil.Result invoke = RetryUtil.invoke(() -> executor.execute(), message.getRetries());
        if (invoke.isSuccess()) {
            return invoke.value();
        }
        //TODO: 后续此处需要记录异常信息，方便处理
        throw invoke.exception();
    }

    /**
     * 预留给子类的扩展钩子函数
     * 调用invokeProcessHandler之后执行该函数
     *
     * @param message
     */
    protected void afterInvoke(M message) {

    }

    /**
     * 子类可以选择重写该方法
     *
     * @param message
     * @return
     */
    protected abstract Object handleBusiness(NotificationMsgRequest message);

    /**
     * 调用消息接手者处理程序
     *
     * @param message
     * @throws IOException
     */
    @Override
    public Object invokeProcessHandler(M message) throws Exception {
        beforeInvoke(message);
        Object result = doProcessHandler(() -> handleBusiness(message), message);
        afterInvoke(message);
        return result;
    }
}
