package org.edt.framework.starter.security.core.model;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Data
public class UserDetailsModel implements UserDetails {
    /**
     * 用户账号
     */
    private String account;
    /**
     * 用户密码
     */
    private String password;
    /**
     * 用户角色
     */
    private String roles;
    /**
     * 用户手机号码
     */
    private String mobile;
    /**
     * 用户名称
     */
    private String userName;
    /**
     * 用户别名
     */
    private String nikeName;
    /**
     * 用户性别
     */
    private String gender;
    /**
     * 用户头像
     */
    private String picture;


    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return account;
    }

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
