package org.etd.framework.starter.pay.merchant;

import org.etd.framework.starter.pay.merchant.dto.SubMerchantApply;

/**
 * 二级商户进件API
 * 微信平台二级商户进件文档地址：https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/ecommerce/applyments/chapter3_1.shtml
 *
 * 支付宝二级商户进件文档地址： https://opendocs.alipay.com/pre-apis/00a8e3
 *
 * @author Young
 * @description
 * @date 2021/1/7
 */
public interface SubMerchantService {

	/**
	 * 创建二级商户信息
	 * @param merchantApply
	 */
	void createMerchant(SubMerchantApply merchantApply);




}
