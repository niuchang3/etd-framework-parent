package org.etd.framework.starter.pay.merchant.properties;

import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * 微信服务商
 *
 * @author Young
 * @description
 * @date 2021/1/8
 */
@Setter
@Configuration
@ConfigurationProperties(prefix = "wx.pay.service.provider")
public class WxServiceProviderProperties implements Serializable {


	/**
	 * 服务商APP_ID
	 */
	protected String appId;
	/**
	 * 服务商APP_SECRET
	 */
	protected String appSecret;
	/**
	 * 服务商商户号
	 */
	protected String mchId;
	/**
	 * 服务商商户秘钥
	 */
	protected String mchKey;
	/**
	 * APIv3密钥
	 */
	protected String apiV3MchKey;

	/**
	 * 证书序列号
	 */
	protected String certSequenceNumber;
	/**
	 * 微信支付安全证书
	 */
	protected String certPath;


	/**
	 * 生成专属
	 * @param subAppId
	 * @param subMchId
	 * @return
	 */
	public WxPayConfig getPayConfig(String subAppId, String subMchId) {
		WxPayConfig config = new WxPayConfig();
		config.setAppId(this.appId);
		config.setMchId(this.mchId);
		config.setMchKey(this.mchKey);
		config.setApiV3Key(this.apiV3MchKey);
		config.setCertSerialNo(this.certSequenceNumber);
		//cert.p12 证书
		config.setKeyPath(this.certPath);
		config.setSubAppId(subAppId);
		config.setSubMchId(subMchId);
		return config;
	}

	/**
	 * 获取微信支付Service
	 * @param subAppId
	 * @param subMchId
	 * @return
	 */
	public WxPayService getPayService(String subAppId, String subMchId){
		WxPayService wxPayService = new WxPayServiceImpl();
		wxPayService.setConfig(getPayConfig(subAppId,mchId));
		return wxPayService;
	}
}
