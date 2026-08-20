package org.etd.upms.service.tenant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.upms.controller.tenant.vo.SystemTenantVO;

import java.util.List;
import java.util.Set;

public interface SystemTenantService {

    List<SystemTenantVO> selectAll();

    List<SystemTenantVO> selectByIds(Set<Long> tenantIds);

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

    /**
     * 修改租户锁定状态
     * @param id
     * @param status
     * @return
     */
    boolean switchLocked(Long id,Boolean status);


    boolean insert();
}
