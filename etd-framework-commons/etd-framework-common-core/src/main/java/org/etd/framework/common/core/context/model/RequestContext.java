package org.etd.framework.common.core.context.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.util.Map;


/**
 * 请求上下文
 *
 * @author 牛昌
 */
@EqualsAndHashCode
@Data
public class RequestContext implements Serializable {

    private static final long serialVersionUID = -1L;

    private static final ThreadLocal<RequestContextModel> REQUEST_CONTEXT = ThreadLocal.withInitial(RequestContextModel::new);

    /**
     * 获取请求上下文
     */
    public static RequestContextModel getRequestContext() {
        return REQUEST_CONTEXT.get();
    }

    /**
     * 清理上下文内容
     */
    public static void clean() {
        RequestContextModel requestContextModel = getRequestContext();
        if (ObjectUtils.isEmpty(requestContextModel)) {
            return;
        }
        requestContextModel.clean();
        REQUEST_CONTEXT.remove();
    }

    /**
     * 复制上下文内容
     */
    public static RequestContextModel copyRequestContext() {
        RequestContextModel requestContext = getRequestContext();
        RequestContextModel copyRequestContext = new RequestContextModel();
        if (ObjectUtils.isEmpty(requestContext)) {
            return copyRequestContext;
        }
        BeanUtils.copyProperties(requestContext, copyRequestContext);
        return copyRequestContext;
    }


    public static String getTraceId() {
        return getRequestContext().getTraceId();
    }

    public static void setTraceId(String traceId) {
        getRequestContext().setTraceId(traceId);
    }

    public static String getRequestIP() {
        return getRequestContext().getRequestIP();
    }

    public static void setRequestIP(String requestIP) {
        getRequestContext().setRequestIP(requestIP);
    }

    public static String getTenantCode() {
        return getRequestContext().getTenantCode();
    }

    public static void setTenantCode(String tenantCode) {
        getRequestContext().setTenantCode(tenantCode);
    }

    public static String getProductCode() {
        return getRequestContext().getProductCode();
    }

    public static void setProductCode(String productCode) {
        getRequestContext().setProductCode(productCode);
    }

    public static String getToken() {
        return getRequestContext().getToken();
    }

    public static void setToken(String token) {
        getRequestContext().setToken(token);
    }

    public static String getUserCode() {
        return getRequestContext().getUserCode();
    }

    public static void setUserCode(String userCode) {
        getRequestContext().setUserCode(userCode);
    }

    public static String getUserName() {
        return getRequestContext().getUserName();
    }

    public static void setUserName(String userName) {
        getRequestContext().setUserName(userName);
    }

    public static String getUserRole() {
        return getRequestContext().getUserRole();
    }

    public static void setUserRole(String userRole) {
        getRequestContext().setUserRole(userRole);
    }

    public static Object getAttribute(String key) {
        return getRequestContext().getAttribute(key);
    }

    public static void setAttribute(String key, Object value) {
        getRequestContext().setAttribute(key, value);
    }


    public static Map<String, Object> getAttribute() {
        return getRequestContext().getAttribute();
    }

    public static void setAttribute(Map<String, Object> attribute) {
        getRequestContext().setAttribute(attribute);
    }
}
