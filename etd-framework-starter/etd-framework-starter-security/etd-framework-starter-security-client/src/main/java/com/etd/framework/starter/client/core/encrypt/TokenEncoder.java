package com.etd.framework.starter.client.core.encrypt;

import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;

/**
 * Token 编码器
 *
 * @param <T>
 */
public interface TokenEncoder<T,R> {

    /**
     * 生成Token
     * @return
     */
    R encode(Oauth2ParameterConstant.TokenType tokenType, T authentication);
}
