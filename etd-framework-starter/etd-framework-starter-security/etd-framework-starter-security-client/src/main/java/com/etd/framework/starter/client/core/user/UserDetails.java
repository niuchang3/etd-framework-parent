package com.etd.framework.starter.client.core.user;

import com.etd.framework.starter.client.core.TenantAuthority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class UserDetails implements Serializable {

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
     * 手机号码
     */
    private String mobile;

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
    private Boolean locked;

    /**
     * 账号是否启用
     */
    private Boolean enabled;


    /**
     * 权限信息
     */
    private List<TenantAuthority> authorities;

}
