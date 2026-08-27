package com.etd.framework.starter.client.core.encrypt;

import com.etd.framework.starter.client.core.constant.SecurityParameterConstant;

/**
 * 令牌编码器。
 *
 * @param <T> 编码输入类型
 * @param <R> 编码结果类型
 */
public interface TokenEncoder<T,R> {

    /**
     * 生成指定类型的令牌。
     *
     * @param tokenType 令牌类型
     * @param authentication 认证信息
     * @return 令牌结果
     */
    R encode(SecurityParameterConstant.TokenType tokenType, T authentication);
}
