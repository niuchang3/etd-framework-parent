package com.etd.framework.starter.client.core.i18n;

import java.util.Locale;

/**
 * Security 消息解析器。
 * <p>
 * 只负责认证和授权链路中的 Message Code 解析，业务系统可提供同类型 Bean 覆盖默认实现。
 */
public interface SecurityMessageResolver {

    /**
     * 根据 Message Code 解析当前语言文案。
     *
     * @param code 消息 Message Code
     * @param args 占位参数
     * @param locale 语言环境
     * @param defaultMessage 解析失败时的默认回退文案
     * @return 解析后的国际化文本
     */
    String resolve(String code, Object[] args, Locale locale, String defaultMessage);
}
