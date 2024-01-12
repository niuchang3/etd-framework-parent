package com.etd.framework.starter.oauth.authentication.converter;

import com.etd.framework.starter.oauth.authentication.constant.Oauth2ParameterConstant;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractAuthenticationConverter implements AuthenticationConverter {


    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!support(request)) {
            return null;
        }
        return doConvert(request);
    }

    protected boolean support(HttpServletRequest request) {
        String grantType = obtainGrantType(request);
        return getGrantType().equals(grantType);
    }


    private String obtainGrantType(HttpServletRequest request) {
        return request.getParameter(Oauth2ParameterConstant.GRANT_TYPE.class.getSimpleName().toLowerCase());
    }

    protected abstract String getGrantType();


    protected abstract Authentication doConvert(HttpServletRequest request);

    /**
     * 从Request中提取参数
     *
     * @param request
     * @param parametersName
     * @return
     */
    protected String obtainRequestParams(HttpServletRequest request, String parametersName) {
        return request.getParameter(parametersName);
    }

    /**
     * scope字符串转 Set集合
     *
     * @param scope
     * @return
     */
    protected Set<String> convertScopeSet(String scope) {
        if (StringUtils.isEmpty(scope)) {
            return null;
        }
        return new HashSet<>(
                Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
    }
}
