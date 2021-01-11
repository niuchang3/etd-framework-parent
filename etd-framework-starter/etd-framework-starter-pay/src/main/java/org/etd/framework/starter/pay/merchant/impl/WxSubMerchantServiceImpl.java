package org.etd.framework.starter.pay.merchant.impl;

import com.github.binarywang.wxpay.bean.applyment.WxPayApplyment4SubCreateRequest;
import com.github.binarywang.wxpay.bean.applyment.WxPayApplyment4SubCreateRequest.*;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.Applyment4SubServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.etd.framework.common.utils.exception.ApiRuntimeException;
import org.etd.framework.common.utils.exception.code.RequestCode;
import org.etd.framework.starter.pay.merchant.SubMerchantService;
import org.etd.framework.starter.pay.merchant.dto.SubMerchantApply;
import org.etd.framework.starter.pay.merchant.dto.impl.WxSubMerchantApply;
import org.etd.framework.starter.pay.merchant.properties.WxServiceProviderProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 微信二级商户进件API
 *
 * @author Young
 * @description
 * @date 2021/1/7
 */
@Slf4j
@Component
public class WxSubMerchantServiceImpl implements SubMerchantService {

	@Autowired
	private WxServiceProviderProperties wxServiceProviderProperties;

	/**
	 * 创建二级商户信息
	 *
	 * @param merchantApply
	 */
	@Override
	public void createMerchant(SubMerchantApply merchantApply) {
		WxPayService payService = wxServiceProviderProperties.getPayService("", "");
		Applyment4SubServiceImpl subService = new Applyment4SubServiceImpl(payService);
		WxSubMerchantApply wxSubMerchantApply = (WxSubMerchantApply) merchantApply;
		try {
			subService.createApply(toWxPayApplyment4SubCreateRequest(wxSubMerchantApply));

		} catch (WxPayException e) {
			String errorMessage = WxPayException.newBuilder()
					.xmlString(e.getXmlString())
					.returnMsg(e.getReturnMsg())
					.returnCode(e.getReturnCode())
					.resultCode(e.getResultCode())
					.errCode(e.getErrCode())
					.errCodeDes(e.getErrCodeDes())
					.buildErrorMsg();

			throw new ApiRuntimeException(RequestCode.FAILED, errorMessage);
		}
	}

	/**
	 * 封装申请二级商户Request
	 *
	 * @param wxSubMerchantApply
	 * @return
	 */
	private WxPayApplyment4SubCreateRequest toWxPayApplyment4SubCreateRequest(WxSubMerchantApply wxSubMerchantApply) {
		WxPayApplyment4SubCreateRequest createRequest = new WxPayApplyment4SubCreateRequest();
		createRequest.setBusinessCode("");
		createRequest.setContactInfo(new ContactInfo());
		createRequest.setSubjectInfo(new SubjectInfo());
		createRequest.setBusinessInfo(new BusinessInfo());
		createRequest.setSettlementInfo(new SettlementInfo());
		createRequest.setBankAccountInfo(new BankAccountInfo());
		createRequest.setAdditionInfo(new AdditionInfo());
		return createRequest;
	}

}
