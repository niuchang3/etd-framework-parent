package com.etd.framework.starter.client.core.encrypt.impl;

import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class JwtTokenDecode implements TokenDecode<SignedJWT> {


    @Getter
    private RSASSAVerifier verifier;

    public JwtTokenDecode(RSAPublicKey publicKey) {
        this.verifier = new RSASSAVerifier(publicKey);
    }


    public SignedJWT decode(String token) throws JOSEException, ParseException {
        SignedJWT parse = SignedJWT.parse(token);
        if (!parse.verify(verifier)) {
            throw new BadCredentialsException("Token校验失败。");
        }
        Date expirationTime = parse.getJWTClaimsSet().getExpirationTime();
        String tokenType = (String) parse.getHeader().getCustomParam(Oauth2ParameterConstant.TokenType.class.getName());

        long now = Calendar.getInstance().getTime().getTime();
        long expired = expirationTime.getTime();
        if (now >= expired) {
            throw new CredentialsExpiredException(tokenType + "Token Credentials Expired");
        }
        return parse;
    }
}
