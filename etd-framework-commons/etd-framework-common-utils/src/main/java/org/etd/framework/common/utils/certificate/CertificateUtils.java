package org.etd.framework.common.utils.certificate;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class CertificateUtils {

    /**
     * 生成公钥私钥
     *
     * @return
     */
    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    /**
     * 将byte[] 数组转换为字符串
     *
     * @param bytes
     * @return
     */
    private static String toString(byte[] bytes) {
        byte[] encodedBytes = Base64.getEncoder().encode(bytes);
        return new String(encodedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 将字符串转换为 byte[] 数组
     *
     * @param str
     * @return
     */
    private static byte[] toByte(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        return Base64.getDecoder().decode(bytes);
    }


    /**
     * base64 字符串 转换公钥
     *
     * @param base64s
     * @return
     */
    public static RSAPublicKey base64ToPublicKey(String base64s) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(toByte(base64s));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    /**
     * base64 字符串转换私钥
     *
     * @param base64s
     * @return
     */
    public static RSAPrivateKey base64ToPrivateKey(String base64s) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(toByte(base64s));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }


    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyPair keyPair = generateRsaKey();
        System.out.println(toString(keyPair.getPublic().getEncoded()));
        System.out.println("----------------private----------------------");
        System.out.println(toString(keyPair.getPrivate().getEncoded()));
    }
}
