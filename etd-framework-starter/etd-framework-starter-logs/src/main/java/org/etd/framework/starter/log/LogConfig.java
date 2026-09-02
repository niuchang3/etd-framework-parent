package org.etd.framework.starter.log;

import org.slf4j.AutoLogMDCAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 日志模块自动配置
 *
 * @author Young
 * @date 2020/12/16
 */
@AutoConfiguration
@ComponentScan({"org.etd.framework.starter.log"})
public class LogConfig {

    /**
     * 在日志自动配置类初始化时主动触发 MDC 适配器安装，确保支持异步线程池上下文透传。
     */
    public LogConfig() {
        AutoLogMDCAdapter.getInstance();
    }

}
