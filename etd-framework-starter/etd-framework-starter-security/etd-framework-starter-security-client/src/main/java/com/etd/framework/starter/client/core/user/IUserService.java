package com.etd.framework.starter.client.core.user;


public interface IUserService {
    /**
     * 根据用户ID查询用户详情
     *
     * @param id
     * @return
     */
    UserDetails loadUserById(Long id);

    /**
     * 根据用户名查询用户
     *
     * @param username
     * @return
     */
    UserDetails loadUserByAccount(String username);

    /**
     * 注册用户
     *
     * @param userDetails
     * @return
     */
    boolean register(UserDetails userDetails);


}
