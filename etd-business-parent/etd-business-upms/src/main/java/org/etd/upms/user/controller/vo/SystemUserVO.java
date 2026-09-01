package org.etd.upms.user.controller.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SystemUserVO {

    /**
     * 用户ID
     */
    private Long id;

    private Date createTime;

    private Date updateTime;

    /**
     * 登录账号
     */
    private String account;
    /**
     * 手机号码
     */
    private String mobile;

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

    private Boolean locked;

    private Boolean enabled;

    private Integer dataStatus;

    /**
     * 角色名称展示文本；未分配时返回“暂未分配”。
     */
    private String roleNames;

    /**
     * 组织名称展示文本；未分配时返回“暂未分配”。
     */
    private String organizationNames;

    private List<SystemUserRoleVO> roles;

    private List<SystemUserOrganizationVO> organizations;

}
