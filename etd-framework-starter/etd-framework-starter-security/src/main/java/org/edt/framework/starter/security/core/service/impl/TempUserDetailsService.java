package org.edt.framework.starter.security.core.service.impl;

import org.edt.framework.starter.security.core.service.IUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 演示使用的用户信息
 */
@Service
public class TempUserDetailsService implements IUserDetailsService {
    //TODO:实际使用时需要进行实际的查询业务

    @Override
    public UserDetails loadUserByMobile(String mobile) throws UsernameNotFoundException {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("user1")
                .password("password")
                .roles("USER")
                .build();
        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username("user1")
                .password("password")
                .roles("USER")
                .build();
        return user;
    }
}
