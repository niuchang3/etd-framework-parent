package com.etd.framework.starter.client.core.converter;

import com.etd.framework.starter.client.core.encrypt.TokenDecode;
import com.etd.framework.starter.client.core.token.BearerTokenAuthentication;
import com.etd.framework.starter.client.core.user.UserDetails;
import com.google.gson.Gson;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;

@Slf4j
public class BearerAuthenticationConverter implements AuthenticationConverter {

    public static final String AUTHENTICATION_SCHEME_BEARER = "Bearer";


    private TokenDecode<SignedJWT> tokenDecode;

    public BearerAuthenticationConverter(TokenDecode<SignedJWT> tokenDecode) {
        this.tokenDecode = tokenDecode;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            return null;
        }

        header = header.trim();
        if (!StringUtils.startsWithIgnoreCase(header, AUTHENTICATION_SCHEME_BEARER)) {
            return null;
        }
        if (header.equalsIgnoreCase(AUTHENTICATION_SCHEME_BEARER)) {
            throw new BadCredentialsException("Empty Bearer authentication token");
        }
        String token = header.substring(7);

        try {
            SignedJWT jwt = (SignedJWT) tokenDecode.decode(token);
            Object user = jwt.getJWTClaimsSet().getClaim(Authentication.class.getName());

            Gson gson = new Gson();
            String json = gson.toJson(user);
            UserDetails userDetails = gson.fromJson(json, UserDetails.class);

            BearerTokenAuthentication authentication = new BearerTokenAuthentication(userDetails.getAuthorities());
            authentication.setDetails(userDetails);

            return authentication;

        } catch (JOSEException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
