package org.etd.framework.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.etd.framework.starter.mybaits.core.BaseEntity;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("system_user")
public class SystemUserEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1181085804651495813L;

    /**
     * 登录账号
     */
    @TableField("ACCOUNT")
    private String account;
    /**
     * 手机号码
     */
    @TableField("MOBILE")
    private String mobile;

    /**
     * 密码信息
     */
    @TableField("PASSWORD")
    private String password;

    /**
     * 用户名称
     */
    @TableField("USER_NAME")
    private String userName;
    /**
     * 生日
     */
    @TableField("BIRTHDAY")
    private Date birthday;
    /**
     * 性别
     */
    @TableField("GENDER")
    private Integer gender;
    /**
     * 头像
     */
    @TableField("AVATAR")
    private String avatar;

    /**
     * 昵称
     */
    @TableField("NICK_NAME")
    private String nickName;

    /**
     * 账号是否锁定
     */
    @TableField("LOCKED")
    private Boolean locked;

    /**
     * 账号是否启用
     */
    @TableField("ENABLED")
    private Boolean enabled;


}
