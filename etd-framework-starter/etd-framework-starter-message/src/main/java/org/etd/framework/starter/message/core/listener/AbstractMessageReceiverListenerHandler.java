package org.etd.framework.starter.message.core.listener;

import org.etd.framework.starter.message.core.model.MessageRequest;

import java.io.IOException;

/**
 * 抽象的消息接受者处理器
 */
public abstract class AbstractMessageReceiverListenerHandler implements MessageReceiverListenerHandler {

    /**
     * 预留给子类的扩展钩子函数
     * 调用invokeProcessHandler之前执行该函数
     *
     * @param messageRequest
     */
    protected void beforeInvoke(MessageRequest messageRequest) {

    }

    protected void doProcessHandler(MessageRequest messageRequest) {

    }

    /**
     * 预留给子类的扩展钩子函数
     * 调用invokeProcessHandler之后执行该函数
     *
     * @param messageRequest
     */
    protected void afterInvoke(MessageRequest messageRequest) {

    }

    /**
     * 调用消息接手者处理程序
     *
     * @param messageRequest
     * @throws IOException
     */
    @Override
    public void invokeProcessHandler(MessageRequest messageRequest) throws IOException {
        beforeInvoke(messageRequest);
        doProcessHandler(messageRequest);
        afterInvoke(messageRequest);
    }
}
