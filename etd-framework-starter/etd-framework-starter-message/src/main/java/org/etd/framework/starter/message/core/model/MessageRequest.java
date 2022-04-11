package org.etd.framework.starter.message.core.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequest<T extends Serializable> implements Serializable {
    /**
     * 消息ID
     */
    private String messageId;
    /**
     * 消息处理类型
     */
    private String messageHandleType;
    /**
     * 消息处理Code
     */
    private String messageHandleCode;

    /**
     * 消息主体
     */
    private T messageBody;
    /**
     * 请求上下文Model
     */
    private RequestContextModel requestContextModel;

}
