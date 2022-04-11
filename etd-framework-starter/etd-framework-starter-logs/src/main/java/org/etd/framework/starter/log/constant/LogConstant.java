package org.etd.framework.starter.log.constant;

import java.io.Serializable;

/**
 * @author Young
 * @description
 * @date 2020/9/15
 */
public class LogConstant implements Serializable {

    /**
     * 日志链路追踪id日志标志
     */
    public static final String LOG_TRACE_ID = "traceId";

    /**
     * 日志类型
     */
    public enum LOG_TYPE {
        /**
         * 系统异常日志
         */
        error,
        /**
         * 访问日志
         */
        access;

    }
}
