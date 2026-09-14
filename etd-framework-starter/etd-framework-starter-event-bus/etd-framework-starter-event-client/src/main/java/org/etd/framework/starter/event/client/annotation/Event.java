package org.etd.framework.starter.event.client.annotation;

import org.etd.framework.event.core.model.EventMessage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明业务方法成功执行后需要发送的事件。
 * <p>
 * {@code payload} 和 {@code partitionKey} 支持 Spring Expression Language（SpEL），常见写法如下：
 * <pre>{@code
 * #result                         // 方法返回值
 * #result.userId                  // 返回对象的 userId 属性
 * #result?.userId                 // 返回值可能为空时安全读取属性
 * #p0 或 #a0                      // 第一个方法参数
 * #command                        // 按参数名引用，工程需保留方法参数名
 * #command.userId                 // 读取参数对象的属性
 * #p0['userId']                   // 读取 Map 参数中的值
 * #command.tenantId + ':' + #result.userId // 拼接分区键
 * }</pre>
 * <p>
 * 方法抛出异常时不会发送事件；存在 Spring 事务时，会在事务成功提交后发送。
 * 注解基于 Spring AOP，同一个类内部通过 {@code this.xxx()} 发起的自调用不会触发。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Event {

    /**
     * 默认以方法返回值作为事件主体。
     */
    String RESULT_EXPRESSION = "#result";

    /**
     * 事件类型，例如 {@code upms.user.created}。
     */
    String type();

    /**
     * 事件协议版本。
     */
    int version() default EventMessage.INITIAL_VERSION;

    /**
     * 用于提取事件主体的 SpEL 表达式。
     */
    String payload() default RESULT_EXPRESSION;

    /**
     * 用于提取 Kafka 分区键的 SpEL 表达式；为空时由发送端使用 eventId。
     */
    String partitionKey() default "";
}
