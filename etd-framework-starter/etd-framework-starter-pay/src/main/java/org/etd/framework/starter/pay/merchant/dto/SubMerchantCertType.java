package org.etd.framework.starter.pay.merchant.dto;

/**
 * 证件类型类型枚举类
 * 微信参考文档：https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/ecommerce/applyments/chapter3_1.shtml
 *
 * @author Young
 * @description
 * @date 2021/1/7
 */
public interface SubMerchantCertType{
	/**
	 * 证件名称
	 * @return
	 */
	String getCertName();

	/**
	 * 证件编号
	 * @return
	 */
	String getCertNumber();



}
