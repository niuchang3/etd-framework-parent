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

    @Override
    public String resolve(String code, Object[] args, Locale locale, String defaultMessage) {
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }
}
