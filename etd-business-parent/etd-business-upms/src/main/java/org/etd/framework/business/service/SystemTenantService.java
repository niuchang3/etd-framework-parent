package org.etd.framework.business.service;

import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.business.vo.SystemTenantVO;
import org.etd.framework.starter.mybaits.tenant.annotation.IgnoreTenant;

import java.util.List;

public interface SystemTenantService {

    /**
     * 查询用户所在租户信息
     *
     * @param user
     * @return
     */

    List<SystemTenantVO> selectByUser(UserDetails user);

    /**
     * 获取当前用户所在租户信息
     *
     * @return
     */
    SystemTenantVO selectCurrentTenant();
}
