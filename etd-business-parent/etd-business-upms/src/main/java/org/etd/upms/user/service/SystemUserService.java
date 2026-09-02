package org.etd.upms.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.etd.upms.user.entity.SystemUserEntity;

import java.util.List;
import java.util.Set;

/**
 * 系统用户Service
 */
public interface SystemUserService {

    IPage<SystemUserEntity> selectUserPage(long current, long size, String keyword, Set<Long> orgIds, Boolean enabled, Boolean locked);

    IPage<SystemUserEntity> page(long current, long size, String keyword, Boolean enabled, Boolean locked,
                                 Set<Long> userIds);
    /**
     * 根据用户ID查询用户信息
     *
     * @param id
     * @return
     */
    SystemUserEntity selectByUserById(Long id);

    SystemUserEntity requireExists(Long id);

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

    Long insert(SystemUserEntity entity, String rawPassword);

    boolean update(Long id, SystemUserEntity entity);

    boolean delete(Long id);

    boolean switchEnabled(Long id, Boolean enabled);

    boolean switchLocked(Long id, Boolean locked);

    Long createTenantAdmin(Long tenantId, String account, String password, String userName, String mobile);

    /**
     * 更新用户主组织机构 ID
     */
    void updatePrimaryOrganization(Long userId, Long primaryOrgId);
}
