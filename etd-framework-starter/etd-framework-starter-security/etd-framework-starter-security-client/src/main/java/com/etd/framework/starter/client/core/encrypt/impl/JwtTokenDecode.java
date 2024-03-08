package com.etd.framework.starter.client.core.encrypt.impl;

import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

public class JwtTokenDecode implements TokenDecode<Authentication> {


    @Getter
    private RSASSAVerifier verifier;

    public JwtTokenDecode(RSAPublicKey publicKey) {
        this.verifier = new RSASSAVerifier(publicKey);
    }


    public Authentication decode(String token) throws JOSEException, ParseException {
        SignedJWT parse = SignedJWT.parse(token);
        if (!parse.verify(verifier)) {
            throw new BadCredentialsException("Token校验失败。");
        }
        return (Authentication) parse.getJWTClaimsSet().getClaim("authentication");
    }
}
