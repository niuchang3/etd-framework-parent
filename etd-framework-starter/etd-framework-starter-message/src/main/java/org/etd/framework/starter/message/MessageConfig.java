package org.etd.framework.starter.message;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author Young
 * @description
 * @date 2020/9/7
 */
@Order(0)
@Configuration
@ComponentScan({"org.etd.framework.starter.message.*"})
public class MessageConfig {

}
