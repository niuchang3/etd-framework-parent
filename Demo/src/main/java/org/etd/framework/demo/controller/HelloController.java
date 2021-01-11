package org.etd.framework.demo.controller;

import org.edt.framework.starter.security.utils.UserDetailUtils;
import org.etd.framework.common.utils.result.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Young
 * @description
 * @date 2020/12/26
 */

@RestController
public class HelloController {

//	@Autowired
//	private WxPayServices wxPayServices;


	@AutoLog("Hello Controller")
	@GetMapping("/permit")
	public ResultModel hello() {
		Object userDetail = UserDetailUtils.getUserDetail();
		return ResultModel.success(userDetail);
	}


	@PreAuthorize("hasAnyAuthority('TEST_1') || hasAnyRole('ADMIN')")
	@AutoLog("Hello Controller")
	@GetMapping("/hello")
	public ResultModel helloController() {
		return ResultModel.success("Hello Controller");
	}

	@GetMapping("/test")
	public void test(@RequestParam String key, @RequestParam String value) {
//		wxPayServices.getWxPayService(key,value);

		System.out.println("123123");
	}

}
