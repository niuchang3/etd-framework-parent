package org.edt.framework.starter.security.dto;

import lombok.Data;

/**
 * @author Young
 * @description
 * @date 2020/12/29
 */
@Data
public class UserPasswordLoginDTO {

	private String account;

	private String password;

	private boolean isRememberMe;
}
