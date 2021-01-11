package org.etd.framework.starter.pay.merchant.dto.impl;

import lombok.Builder;
import lombok.Data;
import org.etd.framework.starter.pay.merchant.dto.SubMerchantApply;

/**
 * @author Young
 * @description
 * @date 2021/1/7
 */
@Data
@Builder
public class WxSubMerchantApply implements SubMerchantApply {



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
		return null;
	}

	/**
	 * 微信参照 organization_type 字段内容中文
	 * 支付宝参照 merchant_type 字段内容中文
	 *
	 * @return
	 */
	@Override
	public String getMerchantName() {
		return null;
	}

	/**
	 * 适用场景描述
	 * 示例值：小微	无营业执照、免办理工商注册登记的商户，需提供小微经营者的个人身份证
	 *
	 * @return
	 */
	@Override
	public String getDescription() {
		return null;
	}

	/**
	 * 证件名称
	 *
	 * @return
	 */
	@Override
	public String getCertName() {
		return null;
	}

	/**
	 * 证件编号
	 *
	 * @return
	 */
	@Override
	public String getCertNumber() {
		return null;
	}
}
