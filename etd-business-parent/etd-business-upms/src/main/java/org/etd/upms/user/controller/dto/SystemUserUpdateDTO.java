package org.etd.upms.user.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SystemUserUpdateDTO {

    @NotBlank(message = "登录账号不能为空")
    @Size(max = 32, message = "登录账号长度不能超过32个字符")
    private String account;

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
}
