package com.etd.framework.starter.client.core.encrypt.impl;

import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import org.springframework.security.core.Authentication;

import java.security.PrivateKey;
import java.util.Calendar;
import java.util.Date;

public class JwtTokeEncoder implements TokenEncoder<Authentication> {


    /**
     * JWT 编码
     */
    @Getter
    private JWSSigner jwsSigner;

    /**
     * Token
     *
     * @param privateKey
     */
    public JwtTokeEncoder(PrivateKey privateKey) {
        this.jwsSigner = new RSASSASigner(privateKey);
    }

    @Override
    public String encoder(Authentication authentication) {
        return encode(null,null,authentication);
    }


    private String encode(String issuer, Date expireTime, Authentication authentication) {
        Calendar now = getNow();
        Date signTime = now.getTime();
        JWTClaimsSet build = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .issueTime(signTime)
                .notBeforeTime(signTime)
                .expirationTime(expireTime)
                .claim("authentication", authentication)
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, build);

        try {
            signedJWT.sign(jwsSigner);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }


    private Calendar getNow() {
        return Calendar.getInstance();
    }

}
