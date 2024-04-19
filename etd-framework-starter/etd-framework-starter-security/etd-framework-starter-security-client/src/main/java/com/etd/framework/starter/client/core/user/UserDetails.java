package com.etd.framework.starter.client.core.user;

import com.etd.framework.starter.client.core.TenantAuthority;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

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
     * 平台管理员
     */
    private Boolean platformAdmin = false;
    /**
     * 租户管理员
     */
    private Boolean tenantAdmin = false;


    /**
     * 权限信息
     */
    private List<TenantAuthority> authorities;


    /**
     * 是否为平台管理员
     *
     * @return
     */
    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    /**
     * 是否租户管理员
     *
     * @return
     */
    public boolean isTenantAdmin() {
        return tenantAdmin;
    }

}
