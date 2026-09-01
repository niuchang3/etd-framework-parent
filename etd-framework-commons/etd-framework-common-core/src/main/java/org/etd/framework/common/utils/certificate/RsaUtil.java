package org.etd.framework.common.utils.certificate;

import cn.hutool.crypto.PemUtil;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class RsaUtil {

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


	public static void keyPairToPem(KeyPair keyPair, String path) {
		Path publickPath = Paths.get(path + File.separator + "rsaPublicKey.pem");
		Path privatePath = Paths.get(path + File.separator + "rsaPrivateKey.pem");
		try (OutputStream publicWriter = Files.newOutputStream(publickPath);
			 OutputStream privateWriter = Files.newOutputStream(privatePath)) {
			PemObject publicKey = new PemObject("PUBLIC KEY", keyPair.getPublic().getEncoded());
			PemObject privateKey = new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded());
			PemUtil.writePemObject(publicKey, publicWriter);
			PemUtil.writePemObject(privateKey, privateWriter);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeyPair keyPair = generateRsaKey();
		File file = new File(System.getProperty("user.dir") + File.separator + "conf");
		if(!file.exists()){
			file.mkdirs();
		}
		keyPairToPem(keyPair, System.getProperty("user.dir") + File.separator + "conf");
	}
}
