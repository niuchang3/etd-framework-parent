package org.etd.framework.common.core.context.model;

import com.etd.framework.starter.client.core.user.UserDetails;
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

    private Long tenantCode;

    private String token;

    private UserDetails userDetails;

    /**
     * 是否忽略租户化查询
     */
    private Boolean ignoreTenant = false;

    private Map<String, Object> attribute = Maps.newLinkedHashMap();

    public void clean() {

        this.traceId = null;
        this.requestIP = null;
        this.tenantCode = null;
        this.token = null;
        this.userDetails = null;
        this.attribute = null;
    }

    public Object getAttribute(String key) {
        return attribute.get(key);
    }

    public void setAttribute(String key, Object value) {
        attribute.put(key, value);
    }
}
