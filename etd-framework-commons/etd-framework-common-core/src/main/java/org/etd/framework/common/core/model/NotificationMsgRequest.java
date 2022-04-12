package org.etd.framework.common.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.etd.framework.common.core.context.model.RequestContextModel;

import java.io.Serializable;

/**
 * Message消息模板
 *
 * @author 牛昌
 * @description
 * @date 2020/9/7
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMsgRequest<T extends Serializable> implements Serializable {


    /**
     * 消息处理Code
     */
    private String messageHandleCode;

    /**
     * 消息主体
     */
    private T messageBody;
    /**
     * 重试次数
     */
    private int retries = 1;
    /**
     * 请求上下文Model
     */
    private RequestContextModel requestContextModel;

}
