package org.etd.framework.business.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;
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

    /**
     * 用户租户
     */
    private List<Tenant> tenant;
}
