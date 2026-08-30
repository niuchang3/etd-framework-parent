package org.etd.upms.user.service;

import com.etd.framework.starter.client.core.user.PermissionsService;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;
import java.util.List;

public interface SystemUserRoleRelService extends PermissionsService {


    /**
     * 根据用户ID查询角色相关信息
     * @param userId
     * @return
     */
    List<SystemUserRoleVO> selectByUser(Long userId);



}
