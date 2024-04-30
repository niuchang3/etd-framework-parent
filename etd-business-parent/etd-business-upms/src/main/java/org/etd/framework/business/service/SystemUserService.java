package org.etd.framework.business.service;

import com.etd.framework.starter.client.core.user.IUserService;
import org.etd.framework.business.entity.SystemUserEntity;

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

    /**
     * 根据账号查询用户信息
     *
     * @param account
     * @return
     */
    SystemUserEntity selectByAccount(String account);
}
