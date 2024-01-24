package com.etd.framework.starter.oauth.authentication.service;

import com.etd.framework.starter.oauth.authentication.entity.Oauth2UserDetails;

public interface UserPasswordService {

    /**
     * 根据用户名查询用户
     *
     * @param name
     * @return
     */
    Oauth2UserDetails loadUserByName(String name);
}
