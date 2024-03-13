package com.etd.framework.starter.client.core.token;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;


public class OauthToken implements Authentication {
    /**
     * Token类型
     */
    @Getter
    @Setter
    private String tokenType;
    /**
     * 访问Token
     */
    @Getter
    @Setter
    private OauthTokenValue accessToken;
    /**
     * 刷新令牌
     */
    @Getter
    @Setter
    private OauthTokenValue refreshToken;

    @JSONField(serialize = false)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @JSONField(serialize = false)
    @Override
    public Object getCredentials() {
        return null;
    }


    @JSONField(serialize = false)
    @Override
    public Object getDetails() {
        return null;
    }

    @JSONField(serialize = false)
    @Override
    public Object getPrincipal() {
        return null;
    }

    @JSONField(serialize = false)
    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @JSONField(serialize = false)
    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

    }

    @JSONField(serialize = false)
    @Override
    public String getName() {
        return null;
    }
}
