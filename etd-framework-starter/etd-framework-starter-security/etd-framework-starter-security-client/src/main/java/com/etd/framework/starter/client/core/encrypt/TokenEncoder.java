package com.etd.framework.starter.client.core.encrypt;

import com.etd.framework.starter.client.core.token.OauthToken;
import com.etd.framework.starter.client.core.user.UserDetails;
import org.springframework.security.core.Authentication;

/**
 * Token 编码器
 *
 * @param <T>
 */
public interface TokenEncoder<T> {

    /**
     * 生成Token
     * @return
     */
    String encoder(T authentication);
}
