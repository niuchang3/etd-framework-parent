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
    /**
     * before Invoke
     *
     * @param message 参数 message
     */
    protected void beforeInvoke(M message) {

    }

    /**
     * do 处理执行 Handler
     *
     * @param executor 参数 executor
     * @param message 参数 message
     * @return 处理结果
     */
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
    /**
     * after Invoke
     *
     * @param message 参数 message
     */
    protected void afterInvoke(M message) {

    }

    /**
     * 子类可以选择重写该方法
     *
     * @param message
     * @return
     */
    /**
     * 执行具体业务事件监听逻辑
     *
     * @param message 抽象消息通知请求对象
     * @return 处理结果
     */
    protected abstract Object executeBusiness(NotificationMsgRequest message);

    /**
     * 调用消息接手者处理程序
     *
     * @param message
     * @throws IOException
     */
    /**
     * invoke 处理执行 Handler
     *
     * @param message 参数 message
     * @return 处理结果
     */
    @Override
    public Object invokeProcessHandler(M message) throws Exception {
        beforeInvoke(message);
        Object result = doProcessHandler(() -> executeBusiness(message), message);
        afterInvoke(message);
        return result;
    }
}
