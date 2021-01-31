package org.edt.framework.starter.security.component;

import org.edt.framework.starter.security.entity.UserDetailModel;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * @author Young
 * @description
 * @date 2021/1/11
 */
@Component
public interface IUserDetailService {

	/**
	 * 通过账号查询用户信息
	 * @param account
	 * @return
	 */
	UserDetailModel loadUserDetailsByAccount(String account);

	/**
	 * 通过手机号码查询账号信息
	 * @param mobile
	 * @return
	 */
	UserDetailModel loadUserDetailsByMobile(String mobile);
}
