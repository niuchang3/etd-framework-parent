package com.etd.framework.starter.client.core.encrypt.impl;

import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SystemOauthProperties;
import com.etd.framework.starter.client.core.token.TokenValue;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import org.springframework.security.core.Authentication;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;


public class JwtTokeEncoder implements TokenEncoder<Authentication, TokenValue> {


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
    public TokenValue encode(String issuer, SystemOauthProperties.Token tokenProperties, Authentication authentication) {
        Calendar now = getNow();
        Date signTime = now.getTime();
        JWTClaimsSet build = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .issueTime(signTime)
                .notBeforeTime(signTime)
                .expirationTime(getExpireTime(tokenProperties))
                .claim(Authentication.class.getName(), authentication)
                .build();
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, build);

        try {
            signedJWT.sign(jwsSigner);
            String token = signedJWT.serialize();
            return new TokenValue(token, build.getExpirationTime());
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }


    private Calendar getNow() {
        return Calendar.getInstance();
    }

    /**
     * 根据配置换算过期时间
     *
     * @param token
     * @return
     */
    private Date getExpireTime(SystemOauthProperties.Token token) {
        Duration duration = Duration.of(token.getExpired(), token.getTimeUnit());
        Instant startInstant = Instant.now();
        Instant instant = startInstant.plusMillis(duration.toMillis());
        return Date.from(instant);
    }

}
