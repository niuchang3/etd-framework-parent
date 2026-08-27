package com.etd.framework.starter.client.core.encrypt.impl;

import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.i18n.SecurityMessageCode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.Assert;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

/**
 * JWT 令牌解码器。
 * <p>
 * 使用 RSA 公钥校验 JWT 签名，签名不通过时直接拒绝认证。
 */
public class JwtTokenDecode implements TokenDecode<SignedJWT> {


    private final RSASSAVerifier verifier;

    /**
     * 创建 JWT 解码器。
     *
     * @param publicKey RSA 公钥
     */
    public JwtTokenDecode(RSAPublicKey publicKey) {
        Assert.notNull(publicKey, "令牌校验公钥不能为空。");
        this.verifier = new RSASSAVerifier(publicKey);
    }


    /**
     * 解析并校验 JWT。
     *
     * @param token JWT 字符串
     * @return 已解析的 JWT
     */
    public SignedJWT decode(String token) throws JOSEException, ParseException {
        SignedJWT parse = SignedJWT.parse(token);
        if (!parse.verify(verifier)) {
            throw new BadCredentialsException(SecurityMessageCode.TOKEN_PARSE_FAILED);
        }
        return parse;
    }
}
