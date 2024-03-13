package com.etd.framework.starter.client.core.encrypt.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.encrypt.TokenEncoder;
import com.etd.framework.starter.client.core.properties.SystemOauthProperties;
import com.etd.framework.starter.client.core.token.OauthTokenValue;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;


public class JwtTokeEncoder implements TokenEncoder<Authentication, OauthTokenValue> {


    /**
     * JWT 编码
     */
    @Getter
    private JWSSigner jwsSigner;

    @Autowired
    private SystemOauthProperties oauthProperties;

    private Snowflake snowflake;

    /**
     * Token
     *
     * @param privateKey
     */
    public JwtTokeEncoder(PrivateKey privateKey) {
        this.jwsSigner = new RSASSASigner(privateKey);
        this.snowflake = IdUtil.createSnowflake(1, 1);
    }

    @Override
    public OauthTokenValue encode(Oauth2ParameterConstant.TokenNameSpace nameSpace, Oauth2ParameterConstant.TokenType tokenType, Authentication authentication) {

        Calendar now = getNow();
        Date signTime = now.getTime();
        JWTClaimsSet build = new JWTClaimsSet.Builder()
                .jwtID(snowflake.nextIdStr())
                .issuer(oauthProperties.getIssuer())
                .issueTime(signTime)
                .notBeforeTime(signTime)
                .expirationTime(getExpireTime(tokenType))
                .claim(Authentication.class.getName(), authentication.getDetails())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .customParam(Oauth2ParameterConstant.TokenType.class.getName(), tokenType.name())
                .customParam(Oauth2ParameterConstant.TokenNameSpace.class.getName(), nameSpace.name())
                .build();

        SignedJWT signedJWT = new SignedJWT(header, build);
        try {
            signedJWT.sign(jwsSigner);
            String token = signedJWT.serialize();
            return new OauthTokenValue(build.getJWTID(), token, build.getExpirationTime());
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
     * @param tokenType
     * @return
     */
    private Date getExpireTime(Oauth2ParameterConstant.TokenType tokenType) {
        SystemOauthProperties.Token token = null;
        if (Oauth2ParameterConstant.TokenType.access_token.equals(tokenType)) {
            token = oauthProperties.getAccessToken();
        }
        if (Oauth2ParameterConstant.TokenType.refresh_token.equals(tokenType)) {
            token = oauthProperties.getRefreshToken();
        }

        Duration duration = Duration.of(token.getExpired(), token.getTimeUnit());
        Instant startInstant = Instant.now();
        Instant instant = startInstant.plusMillis(duration.toMillis());
        return Date.from(instant);
    }

}
