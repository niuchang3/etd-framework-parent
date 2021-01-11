package org.etd.framework.starter.pay.merchant.constants.impl;

import lombok.Getter;
import org.etd.framework.starter.pay.merchant.constants.SubMerchantTypeEnum;

/**
 * 微信二级商户申请主体类型
 *
 * @author Young
 * @description
 * @date 2021/1/7
 */

public enum WXSubMerchantTypeEnum implements SubMerchantTypeEnum {
	/**
	 * 小微商户
	 */
	smallAndMicroMerchants("2401", "小微商户", "无营业执照、免办理工商注册登记的商户、需提供小微经营者的个人身份证"),
	/**
	 * 个人卖家
	 */
	individualSeller("2500", "个人卖家", "无营业执照，已持续从事电子商务经营活动满6个月，且期间经营收入累计超过20万元的个人商家（电商平台需核实已满足上述条件）、需提供个人卖家的个人身份证"),
	/**
	 * 个体工商户
	 */
	individualBusiness("4", "个体工商户", "营业执照上的主体类型一般为个体户、个体工商户、个体经营、需提供营业执照、经营者证件"),
	/**
	 * 企业
	 */
	enterprise("2", "企业", "营业执照上的主体类型一般为有限公司、有限责任公司、需提供营业执照、法人证件、组织机构代码证（未三证合一提供）"),

	/**
	 * 非企业类型
	 */
	nonEnterprise("3","党政、机关及事业单位","包括国内各级、各类政府机构、事业单位等。如：公安、党团、司法、交通、旅游、工商税务、市政、医疗、教育、学校等机构、需提供登记证书、法人证件、组织机构代码证（未三证合一提供）"),

	/**
	 * 其他组织
	 */
	otherOrganizations("1708","其他组织","不属于企业、政府/事业单位的组织机构，如社会团体、民办非企业、基金会。要求机构已办理组织机构代码证。需提供登记证书、法人证件、组织机构代码证（未三证合一提供）");

	@Getter
	private String code;

	@Getter
	private String name;

	@Getter
	private String description;

	WXSubMerchantTypeEnum(String code, String name, String description) {
		this.code = code;
		this.name = name;
		this.description = description;
	}

	/**
	 * 微信参照 organization_type 字段内容
	 * 微信示例值：2401 对照小微商户code
	 * <p>
	 * 支付宝参照 merchant_type 字段内容
	 * 支付宝示例值：01 对照企业code
	 *
	 * @return
	 */
	@Override
	public String getMerchantType() {
		return this.code;
	}

	/**
	 * 微信参照 organization_type 字段内容中文
	 * 支付宝参照 merchant_type 字段内容中文
	 *
	 * @return
	 */
	@Override
	public String getMerchantName() {
		return this.name;
	}

	/**
	 * 适用场景描述
	 * 示例值：小微	无营业执照、免办理工商注册登记的商户，需提供小微经营者的个人身份证
	 *
	 * @return
	 */
	@Override
	public String getDescription() {
		return this.description;
	}
}
