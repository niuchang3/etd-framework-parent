package org.etd.framework.common.core.context.extend;


import com.google.common.collect.Maps;
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
        //此处内容可以降请求参数放到存放进去
        return Maps.newHashMap();
    }

    @Override
    protected String getRemoteIp(HttpServletRequest request) {
        return RequestUtil.getRemoteIp(request);
    }
}
