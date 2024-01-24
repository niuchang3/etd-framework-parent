package com.etd.framework.starter.oauth.authentication.service.impl;

import com.etd.framework.starter.oauth.authentication.entity.Oauth2Authority;
import com.etd.framework.starter.oauth.authentication.entity.Oauth2UserDetails;
import com.etd.framework.starter.oauth.authentication.service.UserPasswordService;
import com.google.common.collect.Lists;
import org.springframework.security.core.userdetails.UserDetails;


public class MemoryUserPasswordServiceImpl implements UserPasswordService {



    @Override
    public Oauth2UserDetails loadUserByName(String name) {
        Oauth2Authority build = Oauth2Authority.builder().authorityCode("read").authorityName("数据读取权限").build();

        Oauth2UserDetails details = Oauth2UserDetails.builder()
                .userName(name)
                .password("123456")
                .authorities(Lists.newArrayList(build))
                .build();
        return details;
    }
}
