package org.etd.framework.common.core.context.extend;


import org.apache.commons.lang3.StringUtils;
import org.etd.framework.common.core.context.AbstractRequestContextInitialization;
import org.etd.framework.common.utils.request.RequestUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * HttpServletRequest请求上下文初始化
 *
 * @author 牛昌
 */
public class HttpServletRequestContextInitializer extends AbstractRequestContextInitialization<HttpServletRequest> {


    @Override
    protected String getHeaderValue(HttpServletRequest request, String headerName) {
        return request.getHeader(headerName);
    }

    @Override
    protected Map<String, Object> getAttribute(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (StringUtils.isEmpty(queryString)) {
            return null;
        }
        String[] queryUnit = queryString.split("&");
        Map<String, Object> params = new HashMap<>();
        for (String query : queryUnit) {
            String[] keyValue = query.split("=");
            params.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : null);
        }
        return params;
    }

    @Override
    protected String getRemoteIp(HttpServletRequest request) {
        return RequestUtil.getRemoteIp(request);
    }
}
