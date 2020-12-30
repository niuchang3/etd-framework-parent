package org.etd.framework.common.utils;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.crypto.asymmetric.SignAlgorithm;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Base64;


@Component
public class SpringContextHelper implements ApplicationContextAware {

	private static ApplicationContext context = null;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}


	public static Object getBean(String name) {
		return context.getBean(name);
	}


	public static Object getBean(Class<?> beanClass) {
		return context.getBean(beanClass);
	}

	public static ApplicationContext getApplicationContext() {
		return context;
	}


	public static void main(String[] args) {
		byte[] signByte = "ETD:NiuChangChang".getBytes();
		Sign sign = SecureUtil.sign(SignAlgorithm.MD5withRSA);
		byte[] signed = sign.sign(signByte);
		String s = Base64.getEncoder().encodeToString(signByte);

//		String s = new String(signed);
//
		System.out.println(s);

//		sign.verify();

	}
}