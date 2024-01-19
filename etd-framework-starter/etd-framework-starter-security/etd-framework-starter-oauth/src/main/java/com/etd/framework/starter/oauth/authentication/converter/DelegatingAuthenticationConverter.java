package com.etd.framework.starter.oauth.authentication.converter;

import com.google.common.collect.Lists;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class DelegatingAuthenticationConverter implements AuthenticationConverter {

    private List<AuthenticationConverter> converters = Lists.newArrayList();

    public DelegatingAuthenticationConverter() {

    }

    public DelegatingAuthenticationConverter addAuthenticationConverter(AuthenticationConverter authenticationConverter) {
        this.converters.add(authenticationConverter);
        return this;
    }


    @Override
    public Authentication convert(HttpServletRequest request) {
        for (AuthenticationConverter converter : converters) {
            Authentication convert = converter.convert(request);
            if (!ObjectUtils.isEmpty(convert)) {
                return convert;
            }
        }
        return null;
    }
}
