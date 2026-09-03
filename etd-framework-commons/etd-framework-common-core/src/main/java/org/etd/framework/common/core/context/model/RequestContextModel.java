package org.etd.framework.common.core.context.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.etd.framework.common.core.user.UserDetails;
import org.etd.framework.common.core.user.PermissionAuthority;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.LinkedHashSet;

/**
 * 统一请求上下文组合数据模型 (Composite Model)
 * 清晰拆分为：消息头上下文（headers）、用户领域模型（userDetails）、治理控制标志（controlFlags）
 *
 * @author 牛昌
 */
@EqualsAndHashCode
@ToString
@Data
public class RequestContextModel implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 1. 协议与传输消息头上下文（可直接无损进行 Header 序列化/导出/跨服务透传）
     */
    private RequestHeaderContext headers = new RequestHeaderContext();

    /**
     * 2. 安全与用户身份领域模型
     */
    private UserDetails userDetails;

    /**
     * 3. 框架与治理控制指令标志
     */
    private RequestControlFlags controlFlags = new RequestControlFlags();

    /**
     * 重置并清理上下文数据
     */
    public void clean() {
        if (this.headers != null) {
            this.headers.clean();
        }
        this.userDetails = null;
        if (this.controlFlags != null) {
            this.controlFlags.clean();
        }
    }

    /**
     * 创建可安全交给异步线程使用的独立上下文副本。
     */
    public RequestContextModel copy() {
        RequestContextModel copy = new RequestContextModel();
        copy.headers = headers == null ? new RequestHeaderContext() : headers.copy();
        copy.userDetails = copyUserDetails(userDetails);
        copy.controlFlags = controlFlags == null ? new RequestControlFlags() : controlFlags.copy();
        return copy;
    }

    private UserDetails copyUserDetails(UserDetails source) {
        if (source == null) {
            return null;
        }
        UserDetails copy = new UserDetails();
        BeanUtils.copyProperties(source, copy);
        // 请求上下文快照不传播登录凭证，避免密码进入异步任务或消息对象。
        copy.setPassword(null);
        copy.setRoleCodes(source.getRoleCodes() == null ? null : new LinkedHashSet<>(source.getRoleCodes()));
        copy.setAuthorities(source.getAuthorities() == null ? null : source.getAuthorities().stream()
                .map(authority -> new PermissionAuthority(authority.getAuthority()))
                .toList());
        return copy;
    }
}
