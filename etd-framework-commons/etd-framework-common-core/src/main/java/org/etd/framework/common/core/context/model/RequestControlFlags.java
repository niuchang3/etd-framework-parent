package org.etd.framework.common.core.context.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 框架与请求控制标志上下文
 * 专职承载单线程执行生命周期内的控制与治理指令标志
 *
 * @author 牛昌
 */
@EqualsAndHashCode
@Data
public class RequestControlFlags implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 是否忽略租户化查询/隔离
     */
    private Boolean ignoreTenant = false;

    /**
     * 是否忽略数据权限过滤
     */
    private Boolean ignoreDataPermission = false;

    /**
     * 重置所有控制标志
     */
    public void clean() {
        this.ignoreTenant = false;
        this.ignoreDataPermission = false;
    }

    public RequestControlFlags copy() {
        RequestControlFlags copy = new RequestControlFlags();
        copy.ignoreTenant = ignoreTenant;
        copy.ignoreDataPermission = ignoreDataPermission;
        return copy;
    }
}
