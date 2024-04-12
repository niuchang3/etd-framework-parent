package org.etd.framework.business.service;

import com.etd.framework.starter.client.core.user.PermissionsService;
import org.etd.framework.business.vo.UserRoleVo;

import java.util.List;

public interface SystemUserRoleRelService extends PermissionsService {


    /**
     * 根据用户ID查询角色相关信息
     * @param userId
     * @return
     */
    List<UserRoleVo> selectByUserId(Long userId);

}
