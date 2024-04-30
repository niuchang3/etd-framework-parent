package org.etd.framework.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.user.UserDetails;
import org.etd.framework.business.vo.SystemTenantVO;

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


    /**
     * 分页查询租户信息
     *
     * @param page
     * @param times
     * @param keyword
     * @return
     */
    IPage<SystemTenantVO> page(IPage page,List<String> times,String keyword);
}
