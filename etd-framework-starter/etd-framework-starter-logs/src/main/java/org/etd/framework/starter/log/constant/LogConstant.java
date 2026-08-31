package org.etd.framework.starter.log.constant;

import lombok.Getter;

import java.io.Serializable;

/**
 * 日志模块专有常量及枚举定义
 *
 * @author Young
 * @date 2020/9/15
 */
public class LogConstant implements Serializable {
    /**
     * MDC 上下文中的日志链路追踪 ID 键名
     */
    public static final String LOG_TRACE_ID = "traceId";

    /**
     * 日志类型
     */
    @Getter
    public enum LOG_TYPE {
        /**
         * 系统异常日志
         */
        ERROR("error"),
        /**
         * 访问日志
         */
        ACCESS("access");

        private final String code;

        LOG_TYPE(String code) {
            this.code = code;
        }
    }
}
