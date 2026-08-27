package com.etd.framework.starter.client.core.encrypt;

import com.nimbusds.jose.JOSEException;

import java.text.ParseException;

/**
 * 令牌解码器。
 *
 * @param <T> 解码结果类型
 */
public interface TokenDecode<T> {

    /**
     * 解码令牌并返回解析结果。
     *
     * @param token 令牌原文
     * @return 令牌解析结果
     */
    T decode(String token) throws JOSEException, ParseException;
}
