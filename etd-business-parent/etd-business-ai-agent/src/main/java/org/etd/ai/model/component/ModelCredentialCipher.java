package org.etd.ai.model.component;

import lombok.RequiredArgsConstructor;

import org.etd.ai.model.config.ModelCredentialProperties;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** 租户凭证认证加密，AAD将密文绑定到租户及连接ID，日志不输出密钥。 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(ModelCredentialProperties.class)
public class ModelCredentialCipher {
    private final ModelCredentialProperties properties;

    /** AES-256主密钥的字节数。 */
    private static final int KEY_BYTES = 32;

    /** GCM推荐随机nonce的字节数。 */
    private static final int NONCE_BYTES = 12;

    /** GCM认证标签位数。 */
    private static final int TAG_BITS = 128;

    /**
     * 加密并返回包含随机nonce的封装；没有配置主密钥时拒绝保存API Key。
     *
     * @param value 待加密凭证或配置值，不写入日志或普通响应
     * @param tenantId 后端确定的操作租户标识
     * @param connectionId 所属模型服务连接标识
     * @return 含随机nonce和认证标签的AES-GCM密文封装
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public String encrypt(String value, Long tenantId, Long connectionId) {
        if (properties.getKey() == null || properties.getKey().isBlank()) {
            throw new ApiRuntimeException("请配置服务端模型凭证加密主密钥");
        }
        try {
            byte[] key = Base64.getDecoder().decode(properties.getKey());
            if (key.length != KEY_BYTES
                    || properties.getKeyId() == null
                    || properties.getKeyId().isBlank()) {
                throw new IllegalArgumentException("未配置有效主密钥");
            }
            byte[] nonce = new byte[NONCE_BYTES];
            new SecureRandom().nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            GCMParameterSpec parameters = new GCMParameterSpec(TAG_BITS, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameters);
            cipher.updateAAD((tenantId + ":" + connectionId).getBytes(StandardCharsets.UTF_8));
            return "v1."
                    + Base64.getEncoder().encodeToString(nonce)
                    + "."
                    + Base64.getEncoder()
                            .encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new ApiRuntimeException("模型凭证加密不可用，请配置服务端主密钥");
        }
    }

    /**
     * 在模型调用边界解密凭证，不允许将明文返回接口或写入日志。
     * @param value AES-GCM密文
     * @param keyId 密钥标识
     * @param tenantId 凭证所属租户
     * @param connectionId 凭证所属连接器
     * @return 仅供服务端请求使用的明文密钥
     */
    public String decrypt(String value, String keyId, Long tenantId, Long connectionId) {
        try {
            if (!requireKeyId().equals(keyId)) {
                throw new IllegalArgumentException("密钥标识不匹配");
            }
            String[] parts = value.split("\\.", -1);
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                throw new IllegalArgumentException("密文格式无效");
            }
            byte[] key = Base64.getDecoder().decode(properties.getKey());
            byte[] nonce = Base64.getDecoder().decode(parts[1]);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            GCMParameterSpec parameters = new GCMParameterSpec(TAG_BITS, nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameters);
            cipher.updateAAD((tenantId + ":" + connectionId).getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new ApiRuntimeException("模型凭证解密失败，请重新配置连接器");
        }
    }

    /**
     * 返回非敏感的当前密钥标识。
     *
     * @return 当前服务端主密钥的非敏感标识
     * @throws ApiRuntimeException 业务约束或可信上下文校验不满足时
     */
    public String requireKeyId() {
        if (properties.getKeyId() == null || properties.getKeyId().isBlank()) {
            throw new ApiRuntimeException("请配置模型凭证主密钥标识");
        }
        return properties.getKeyId();
    }
}
