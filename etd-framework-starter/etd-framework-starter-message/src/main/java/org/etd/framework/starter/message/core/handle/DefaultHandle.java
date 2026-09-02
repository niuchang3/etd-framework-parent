package org.etd.framework.starter.message.core.handle;

import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.common.core.listener.AbstractListenerHandler;
import org.etd.framework.common.core.model.NotificationMsgRequest;
import org.springframework.stereotype.Component;

/**
 * @author Administrator
 */
@Slf4j
@Component("defaultHandle")
public class DefaultHandle extends AbstractListenerHandler {

    /**
     * 处理 Business
     *
     * @param message 参数 message
     * @return 处理结果
     */
    @Override
    protected Object handleBusiness(NotificationMsgRequest message) {
        log.info("处理默认消息，消息编码：{}，链路标识：{}", message.getMessageHandleCode(), RequestContext.getTraceId());
        return null;
    }
}
