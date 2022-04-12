package org.etd.framework.starter.message.core.annotation;

import java.lang.annotation.*;

/**
 * @author 事件触发器
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Event {

    String messageHandleCode() default "defaultHandle";

    int retries() default 1;
}
