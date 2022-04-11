package org.etd.framework.starter.message.core.config;

import org.etd.framework.common.core.constants.RequestContextConstant;
import org.etd.framework.common.core.context.model.RequestContext;
import org.etd.framework.starter.message.core.context.RabbitMQRequestContextInitializer;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Correlation;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;

/**
 * RabbitMQ Config
 */
public class RabbitMQConfig extends RabbitMQRequestContextInitializer {

    /**
     * 默认将线程ID传递到下游监听者下游监听者方便日志监听
     * 上游消息生产生：
     * Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
     * message.getMessageProperties().setHeader(LogConstant.TRACE_ID_HEADER, copyOfContextMap.get(LogConstant.LOG_TRACE_ID));
     * <p>
     * 下游消息消费者：
     * String traceId = (String) headers.get(LogConstant.TRACE_ID_HEADER);
     * Map<String, String> contextMap = new HashMap<>();
     * contextMap.put(LogConstant.LOG_TRACE_ID, traceId);
     * MDC.setContextMap(contextMap);
     *
     * @param connectionFactory
     * @return
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        template.setConnectionFactory(connectionFactory);

        template.setBeforePublishPostProcessors(new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                setRabbitMqMessageHeads(message);
                return message;
            }

            @Override
            public Message postProcessMessage(Message message, Correlation correlation) {
                setRabbitMqMessageHeads(message);
                return message;
            }

            @Override
            public Message postProcessMessage(Message message, Correlation correlation, String exchange, String routingKey) {
                setRabbitMqMessageHeads(message);
                return message;
            }
        });
        template.setAfterReceivePostProcessors(
                new MessagePostProcessor() {

                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        initialization(message);
                        return message;
                    }

                    @Override
                    public Message postProcessMessage(Message message, Correlation correlation) {
                        initialization(message);
                        return message;
                    }

                    @Override
                    public Message postProcessMessage(Message message, Correlation correlation, String exchange, String routingKey) {
                        initialization(message);
                        return message;
                    }
                }
        );

        return template;
    }


    public void setRabbitMqMessageHeads(Message message) {
        message.getMessageProperties().setHeader(RequestContextConstant.TRACE_ID.getCode(), RequestContext.getTraceId());
        message.getMessageProperties().setHeader(RequestContextConstant.REQUEST_IP.getCode(), RequestContext.getRequestIP());
        message.getMessageProperties().setHeader(RequestContextConstant.TENANT_CODE.getCode(), RequestContext.getTenantCode());
        message.getMessageProperties().setHeader(RequestContextConstant.PRODUCT_CODE.getCode(), RequestContext.getProductCode());
        message.getMessageProperties().setHeader(RequestContextConstant.TOKEN.getCode(), RequestContext.getTenantCode());
        message.getMessageProperties().setHeader(RequestContextConstant.USER_CODE.getCode(), RequestContext.getUserCode());
        message.getMessageProperties().setHeader(RequestContextConstant.USER_NAME.getCode(), RequestContext.getUserName());
        message.getMessageProperties().setHeader(RequestContextConstant.USER_ROLE.getCode(), RequestContext.getUserRole());
    }
}
