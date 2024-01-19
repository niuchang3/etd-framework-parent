package com.etd.framework.starter.oauth.authentication.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Oauth2UserDetails implements Serializable {

    private static final long serialVersionUID = -1L;


    /**
     * 用户ID
     */
    private Long id;
    /**
     * 登录账号
     */
    private String account;

    /**
     * 密码信息
     */

    private String password;

    /**
     * 用户名称
     */
    private String userName;
    /**
     * 生日
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
    /**
     * 性别
     */
    private Integer gender;
    /**
     * 头像
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 账号是否锁定
     */
    private Boolean locked = false;
    /**
     * 账号是否过期
     */
    private Boolean accountNonExpired = false;
    /**
     * 密码是否过期
     */
    private Boolean passwordExpired = false;
    /**
     * 账号是否启用
     */
    private Boolean enabled = true;
    /**
     * 权限信息
     */
    private List<Oauth2Authority> authorities;


}
