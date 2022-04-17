package com.etd.framework.authorization.password;

import com.etd.framework.utils.OAuth2EndpointUtils;
import com.google.common.collect.Sets;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UserPasswordTokenAuthenticationConverter implements AuthenticationConverter {


    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!AuthorizationGrantType.PASSWORD.getValue().equals(grantType)) {
            return null;
        }
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getParameters(request);

        String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
        Set<String> requestedScopes = null;
        if (StringUtils.hasText(scope)) {
            requestedScopes = Sets.newHashSet(StringUtils.delimitedListToStringArray(scope, " "));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return new UserPasswordAuthenticationToken(AuthorizationGrantType.PASSWORD, requestedScopes, authentication, getAdditionalParameters(parameters));
    }

    /**
     * 获取附加参数
     *
     * @param parameters
     * @return
     */
    private Map<String, Object> getAdditionalParameters(MultiValueMap<String, String> parameters) {
        return parameters
                .entrySet()
                .stream()
                .filter(e -> !e.getKey().equals(OAuth2ParameterNames.GRANT_TYPE) &&
                        !e.getKey().equals(OAuth2ParameterNames.SCOPE))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
    }


}
