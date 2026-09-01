package org.etd.upms.user.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class SystemUserCreateDTO {

    @NotBlank(message = "登录账号不能为空")
    @Size(max = 32, message = "登录账号长度不能超过32个字符")
    private String account;

    @NotBlank(message = "登录密码不能为空")
    @Size(min = 8, max = 72, message = "登录密码长度必须在8到72个字符之间")
    private String password;

    @NotBlank(message = "用户名称不能为空")
    @Size(max = 20, message = "用户名称长度不能超过20个字符")
    private String userName;

    @Size(max = 20, message = "手机号码长度不能超过20个字符")
    private String mobile;

    private LocalDate birthday;

    private Integer gender;

    @Size(max = 200, message = "头像地址长度不能超过200个字符")
    private String avatar;

    @Size(max = 100, message = "昵称长度不能超过100个字符")
    private String nickName;

    private Set<Long> roleIds = new LinkedHashSet<>();

    private Set<Long> organizationIds = new LinkedHashSet<>();

    private Long primaryOrganizationId;
}
