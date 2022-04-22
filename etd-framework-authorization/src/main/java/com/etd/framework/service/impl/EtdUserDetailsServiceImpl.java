package com.etd.framework.service.impl;

import com.etd.framework.service.EtdUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class EtdUserDetailsServiceImpl implements EtdUserDetailsService {


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if ("user1".equals(username)) {
            return User.withDefaultPasswordEncoder()
                    .username("user1")
                    .password("password")
                    .roles("USER")
                    .build();
        }
        if ("user2".equals(username)) {
            return User.withDefaultPasswordEncoder()
                    .username("user2")
                    .password("password")
                    .roles("ROLE")
                    .build();
        }
        return null;
    }
}
