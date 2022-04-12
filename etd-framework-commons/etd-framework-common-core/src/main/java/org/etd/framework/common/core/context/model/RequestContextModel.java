package org.etd.framework.common.core.context.model;

import com.google.common.collect.Maps;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;

@EqualsAndHashCode
@ToString
@Data
public class RequestContextModel implements Serializable {


    private static final long serialVersionUID = -1L;

    private String traceId;

    private String requestIP;

    private String tenantCode;

    private String productCode;

    private String token;

    private String userCode;

    private String userName;

    private String userRole;

    private Map<String, Object> attribute = Maps.newLinkedHashMap();

    public void clean() {
        this.traceId = null;
        this.requestIP = null;
        this.tenantCode = null;
        this.productCode = null;
        this.token = null;
        this.userCode = null;
        this.userName = null;
        this.attribute = null;
    }

    public Object getAttribute(String key) {
        return attribute.get(key);
    }

    public void setAttribute(String key, Object value) {
        attribute.put(key, value);
    }
}
