package com.etd.framework.starter.client.core.provider;

import com.etd.framework.starter.client.core.constant.Oauth2ParameterConstant;
import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.storage.TokenStorage;
import com.etd.framework.starter.client.core.token.BearerTokenAuthentication;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.google.gson.Gson;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.starter.cache.RedisCache;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

@Slf4j
public class BearerAuthenticationProvider implements AuthenticationProvider {


    private TokenDecode<SignedJWT> tokenDecode;


    public BearerAuthenticationProvider(TokenDecode<SignedJWT> tokenDecode) {
        this.tokenDecode = tokenDecode;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        BearerTokenAuthentication tokenAuthentication = (BearerTokenAuthentication) authentication;
        try {
            SignedJWT jwt = (SignedJWT) tokenDecode.decode(tokenAuthentication.getCredentials());
            verifyExpired(jwt);


            Object user = jwt.getJWTClaimsSet().getClaim(Authentication.class.getName());
            String namespace = (String) jwt.getHeader().getCustomParam(Oauth2ParameterConstant.TokenNameSpace.class.getName());
            Gson gson = new Gson();
            String json = gson.toJson(user);
            UserDetails userDetails = gson.fromJson(json, UserDetails.class);
            boolean existAccessToken = TokenStorage.isExistAccessToken(namespace,String.valueOf(userDetails.getId()), jwt.getJWTClaimsSet().getJWTID());
            if (!existAccessToken) {
                throw new CredentialsExpiredException("Token be revoked");
            }

            BearerTokenAuthentication newAuthentication = new BearerTokenAuthentication(userDetails.getAuthorities());
            newAuthentication.setAuthenticated(true);
            newAuthentication.setDetails(userDetails);
            return newAuthentication;

        } catch (JOSEException | ParseException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return BearerTokenAuthentication.class.isAssignableFrom(authentication);
    }

    private void verifyExpired(SignedJWT jwt) throws ParseException {
        Date expirationTime = jwt.getJWTClaimsSet().getExpirationTime();
        String tokenType = (String) jwt.getHeader().getCustomParam(Oauth2ParameterConstant.TokenType.class.getName());
        long now = Calendar.getInstance().getTime().getTime();
        long expired = expirationTime.getTime();
        if (now >= expired) {
            throw new CredentialsExpiredException(tokenType + "Token Credentials Expired");
        }
    }
}
