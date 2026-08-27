package com.etd.framework.starter.client.core.user;

/**
 * 用户信息加载服务。
 * <p>
 * 业务系统应提供该接口实现，用于登录校验和刷新令牌时重新加载用户信息。
 */
public interface IUserService {
    /**
     * 根据用户标识查询用户详情。
     *
     * @param id 用户标识
     * @return 用户详情
     */
    UserDetails loadUserById(Long id);

    /**
     * 根据登录账号查询用户。
     *
     * @param username 登录账号
     * @return 用户详情
     */
    UserDetails loadUserByAccount(String username);

    /**
     * 注册用户。
     *
     * @param userDetails 用户详情
     * @return 注册结果
     */
    boolean register(UserDetails userDetails);


}
