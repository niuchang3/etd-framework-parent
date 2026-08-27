package com.etd.framework.starter.client.core.encrypt;

import cn.hutool.crypto.PemUtil;
import com.etd.framework.starter.client.core.properties.SecurityProperties;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * 安全密钥加载器。
 * <p>
 * 统一处理 RSA 公私钥配置路径解析和 PEM 文件读取，避免认证服务端和客户端重复维护路径查找逻辑。
 */
public final class SecurityKeyLoader {

    private static final String CLASSPATH_PREFIX = "classpath:";

    private static final String DEFAULT_PUBLIC_KEY_LOCATION = "conf/rsaPublicKey.pem";

    private static final String DEFAULT_PRIVATE_KEY_LOCATION = "conf/rsaPrivateKey.pem";

    private SecurityKeyLoader() {
    }

    /**
     * 从安全配置读取 RSA 公钥。
     *
     * @param securityProperties 安全配置
     * @return RSA 公钥
     */
    public static RSAPublicKey loadPublicKey(SecurityProperties securityProperties) {
        String location = getPublicKeyLocation(securityProperties);
        try (InputStream inputStream = openInputStream(location, "公钥")) {
            return (RSAPublicKey) PemUtil.readPemPublicKey(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("读取公钥配置失败。", exception);
        }
    }

    /**
     * 从安全配置读取 RSA 私钥。
     *
     * @param securityProperties 安全配置
     * @return RSA 私钥
     */
    public static PrivateKey loadPrivateKey(SecurityProperties securityProperties) {
        String location = getPrivateKeyLocation(securityProperties);
        try (InputStream inputStream = openInputStream(location, "私钥")) {
            return PemUtil.readPemPrivateKey(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("读取私钥配置失败。", exception);
        }
    }

    /**
     * 从配置中获取公钥路径。
     *
     * @param securityProperties 安全配置
     * @return 公钥文件路径
     */
    private static String getPublicKeyLocation(SecurityProperties securityProperties) {
        if (securityProperties.getKey() == null || !StringUtils.hasText(securityProperties.getKey().getPublicKeyPath())) {
            return DEFAULT_PUBLIC_KEY_LOCATION;
        }
        return securityProperties.getKey().getPublicKeyPath();
    }

    /**
     * 从配置中获取私钥路径。
     *
     * @param securityProperties 安全配置
     * @return 私钥文件路径
     */
    private static String getPrivateKeyLocation(SecurityProperties securityProperties) {
        if (securityProperties.getKey() == null || !StringUtils.hasText(securityProperties.getKey().getPrivateKeyPath())) {
            return DEFAULT_PRIVATE_KEY_LOCATION;
        }
        return securityProperties.getKey().getPrivateKeyPath();
    }

    /**
     * 打开密钥输入流。
     * <p>
     * classpath: 前缀按类路径资源读取；其他路径按文件系统路径读取。
     *
     * @param location 配置文件路径
     * @param keyName 密钥名称
     * @return 密钥输入流
     */
    private static InputStream openInputStream(String location, String keyName) throws IOException {
        if (location.startsWith(CLASSPATH_PREFIX)) {
            return openClasspathInputStream(location.substring(CLASSPATH_PREFIX.length()), keyName);
        }
        return Files.newInputStream(resolveConfFile(location, keyName));
    }

    /**
     * 从类路径读取密钥资源。
     *
     * @param resourceLocation 类路径资源位置
     * @param keyName 密钥名称
     * @return 密钥输入流
     */
    private static InputStream openClasspathInputStream(String resourceLocation, String keyName) throws IOException {
        String normalizedLocation = normalizeClasspathLocation(resourceLocation);
        InputStream inputStream = getResourceAsStream(normalizedLocation);
        if (inputStream != null) {
            return inputStream;
        }
        throw new IOException("未找到配置的" + keyName + "类路径资源：" + normalizedLocation);
    }

    /**
     * 标准化类路径资源位置。
     *
     * @param resourceLocation 类路径资源位置
     * @return 标准化后的类路径资源位置
     */
    private static String normalizeClasspathLocation(String resourceLocation) {
        String normalizedLocation = resourceLocation;
        while (normalizedLocation.startsWith("/")) {
            normalizedLocation = normalizedLocation.substring(1);
        }
        return normalizedLocation;
    }

    /**
     * 获取类路径资源输入流。
     *
     * @param resourceLocation 类路径资源位置
     * @return 类路径资源输入流
     */
    private static InputStream getResourceAsStream(String resourceLocation) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            InputStream inputStream = contextClassLoader.getResourceAsStream(resourceLocation);
            if (inputStream != null) {
                return inputStream;
            }
        }
        return SecurityKeyLoader.class.getClassLoader().getResourceAsStream(resourceLocation);
    }

    /**
     * 解析密钥文件路径。
     * <p>
     * 绝对路径直接使用；相对路径会优先按当前运行目录解析，找不到时再从当前目录向上查找。
     *
     * @param location 配置文件路径
     * @param keyName 密钥名称
     * @return 配置文件路径
     */
    private static Path resolveConfFile(String location, String keyName) throws IOException {
        Path configured = Paths.get(location);
        if (configured.isAbsolute()) {
            if (Files.exists(configured)) {
                return configured;
            }
            throw new IOException("未找到配置的" + keyName + "文件：" + configured);
        }

        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(configured);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("从当前目录向上查找，未找到配置文件 " + location + "，当前目录：" + System.getProperty("user.dir"));
    }
}
