package com.etd.framework.starter.client.core.encrypt;

import com.nimbusds.jose.JOSEException;

import java.text.ParseException;

/**
 * Token解码器
 *
 * @param <T>
 */
public interface TokenDecode<T> {

    /**
     * Token解码后返回的内容
     *
     * @param token
     * @return
     */
    T decode(String token) throws JOSEException, ParseException;
}
