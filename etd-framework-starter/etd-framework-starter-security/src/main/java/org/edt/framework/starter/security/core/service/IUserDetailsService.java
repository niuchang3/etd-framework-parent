package org.edt.framework.starter.security.core.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface IUserDetailsService extends UserDetailsService {

    /**
     * 根据手机号码查询用户
     *
     * @param mobile
     * @return
     * @throws UsernameNotFoundException
     */
    UserDetails loadUserByMobile(String mobile) throws UsernameNotFoundException;
}
