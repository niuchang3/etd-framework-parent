package org.etd.upms.user.service;

import com.etd.framework.starter.client.core.user.IUserService;
import org.etd.upms.user.entity.SystemUserEntity;

import java.util.List;
import java.util.Set;

/**
 * 系统用户Service
 */
public interface SystemUserService extends IUserService {
    /**
     * 根据用户ID查询用户信息
     *
     * @param id
     * @return
     */
    SystemUserEntity selectByUserById(Long id);

    /**
     * 根据ID集合查询用户
     *
     * @param ids
     * @return
     */
    List<SystemUserEntity> selectByUserById(Set<Long> ids);

    Set<Long> selectUserIdsByTenantId(Long tenantId);

    /**
     * 根据账号查询用户信息
     *
     * @param account
     * @return
     */
    SystemUserEntity selectByAccount(String account);

    Long createTenantAdmin(Long tenantId, String account, String password, String userName, String mobile);
}
