package org.edt.framework.starter.security.controller;

import org.edt.framework.starter.security.component.IUserDetailService;
import org.edt.framework.starter.security.constants.SecurityConstants;
import org.edt.framework.starter.security.dto.UserMobileLoginDTO;
import org.edt.framework.starter.security.dto.UserPasswordLoginDTO;
import org.edt.framework.starter.security.entity.UserDetailModel;
import org.edt.framework.starter.security.token.JwtToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Young
 * @description
 * @date 2020/12/29
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private IUserDetailService userDetailsService;

	/**
	 * 账号密码登录
	 * @param dto
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/login")
	public ResponseEntity<Void> loginByAccount(@RequestBody UserPasswordLoginDTO dto) {

		UserDetailModel userDetails = userDetailsService.loadUserDetailsByAccount(dto.getAccount());
		String token = JwtToken.createToken(redisTemplate, userDetails, dto.isRememberMe());
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(SecurityConstants.TOKEN_HEADER, token);
		return new ResponseEntity<>(httpHeaders, HttpStatus.OK);
	}

	/**
	 * 短信登录
	 * TODO 需要完善功能
	 * @param dto
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/sms")
	public ResponseEntity<Void> loginByMobile(@RequestBody UserMobileLoginDTO dto) {

		UserDetailModel userDetails = userDetailsService.loadUserDetailsByMobile(dto.getMobile());
		String token = JwtToken.createToken(redisTemplate, userDetails, false);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(SecurityConstants.TOKEN_HEADER, token);
		return new ResponseEntity<>(httpHeaders, HttpStatus.OK);
	}
}
