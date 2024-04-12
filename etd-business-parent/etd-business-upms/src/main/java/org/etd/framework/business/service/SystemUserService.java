package org.etd.framework.business.service;

import com.etd.framework.starter.client.core.user.IUserService;
import org.etd.framework.business.entity.SystemUserEntity;

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
     * 根据账号查询用户信息
     *
     * @param account
     * @return
     */
    SystemUserEntity selectByAccount(String account);
}
