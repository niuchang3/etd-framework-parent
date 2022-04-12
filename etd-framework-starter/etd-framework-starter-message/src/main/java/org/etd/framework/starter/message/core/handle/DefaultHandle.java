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

    @Override
    protected Object handleBusiness(NotificationMsgRequest message) {
        log.info(message.toString());
        log.info(RequestContext.getRequestContext().toString());
        return null;
    }
}
