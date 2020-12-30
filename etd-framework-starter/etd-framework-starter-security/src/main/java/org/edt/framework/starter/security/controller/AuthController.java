package org.edt.framework.starter.security.controller;

import org.edt.framework.starter.security.constants.SecurityConstants;
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

import java.util.Arrays;

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


	@RequestMapping(method = RequestMethod.POST, value = "/login")
	public ResponseEntity<Void> login(@RequestBody UserPasswordLoginDTO dto) {
		UserDetailModel userDetailModel = UserDetailModel.builder()
				.id("123456789")
				.name("张三")
				.account("zhangsan")
				.password("123321")
				.roles(Arrays.asList("ADMIN2"))
				.permissions(Arrays.asList("TEST_3", "TEST4"))
				.build();
		String token = JwtToken.createToken(redisTemplate, userDetailModel, true);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set(SecurityConstants.TOKEN_HEADER, token);
		return new ResponseEntity<>(httpHeaders, HttpStatus.OK);
	}
}
