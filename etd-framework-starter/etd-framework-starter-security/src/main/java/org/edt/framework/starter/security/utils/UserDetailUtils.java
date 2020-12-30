package org.edt.framework.starter.security.utils;

import org.edt.framework.starter.security.entity.UserDetailModel;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Young
 * @description
 * @date 2020/12/26
 */
public class UserDetailUtils {


	/**
	 * 获取用户详细信息
	 *
	 * @return
	 */
	public static UserDetailModel getUserDetail() {
		return (UserDetailModel) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}


}
