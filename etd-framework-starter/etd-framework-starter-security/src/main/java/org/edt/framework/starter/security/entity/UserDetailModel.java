package org.edt.framework.starter.security.entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Young
 * @description
 * @date 2020/12/29
 */
@Data
@Builder
public class UserDetailModel implements Serializable {

	private String id;

	private String name;

	private String userImage;

	private String mobile;

	private String email;

	private String account;

	private String password;

	private List<String> roles;

	private List<String> permissions;
}
