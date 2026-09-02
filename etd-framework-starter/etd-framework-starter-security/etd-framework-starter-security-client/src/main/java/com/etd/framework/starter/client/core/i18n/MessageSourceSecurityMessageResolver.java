package com.etd.framework.starter.client.core.i18n;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;

import java.util.Locale;

/**
 * 基于 Spring MessageSource 的 Security 消息解析器。
 */
@RequiredArgsConstructor
public class MessageSourceSecurityMessageResolver implements SecurityMessageResolver {

    private final MessageSource messageSource;

    /**
     * resolve
     *
     * @param code 参数 code
     * @param args 参数 args
     * @param locale 参数 locale
     * @param defaultMessage 参数 defaultMessage
     * @return 处理结果
     */
    @Override
    public String resolve(String code, Object[] args, Locale locale, String defaultMessage) {
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }
}
