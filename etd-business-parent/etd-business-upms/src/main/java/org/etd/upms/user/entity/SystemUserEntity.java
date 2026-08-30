package org.etd.upms.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.BaseEntity;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user")
public class SystemUserEntity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1181085804651495813L;

    /**
     * 登录账号
     */
    @TableField("account")
    private String account;
    /**
     * 手机号码
     */
    @TableField("mobile")
    private String mobile;

    /**
     * 密码信息
     */
    @TableField("password")
    private String password;

    /**
     * 用户名称
     */
    @TableField("user_name")
    private String userName;
    /**
     * 生日
     */
    @TableField("birthday")
    private Date birthday;
    /**
     * 性别
     */
    @TableField("gender")
    private Integer gender;
    /**
     * 头像
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 昵称
     */
    @TableField("nick_name")
    private String nickName;

    /**
     * 账号是否锁定
     */
    @TableField("locked")
    private Boolean locked;

    /**
     * 账号是否启用
     */
    @TableField("enabled")
    private Boolean enabled;


}
