package org.etd.framework.starter.pay.merchant.constants;

/**
 * 申请开通二级商户类型
 * <p>
 * 微信商户进件文档地址： https://pay.weixin.qq.com/wiki/doc/apiv3/wxpay/ecommerce/applyments/chapter3_1.shtml
 * <p>
 * 支付宝商户进件文档地址: https://opendocs.alipay.com/pre-apis/00a8e3
 *
 * @author Young
 * @description SubMerchantTypeEnum
 * @date 2021/1/7
 */
public interface SubMerchantTypeEnum {

	/**
	 * 微信参照 organization_type 字段内容
	 * 微信示例值：2401 对照小微商户code
	 * <p>
	 * 支付宝参照 merchant_type 字段内容
	 * 支付宝示例值：01 对照企业code
	 *
	 * @return
	 */
	String getMerchantType();

	/**
	 * 微信参照 organization_type 字段内容中文
	 * 支付宝参照 merchant_type 字段内容中文
	 *
	 * @return
	 */
	String getMerchantName();

	/**
	 * 适用场景描述
	 * 示例值：小微	无营业执照、免办理工商注册登记的商户，需提供小微经营者的个人身份证
	 *
	 * @return
	 */
	String getDescription();
}
