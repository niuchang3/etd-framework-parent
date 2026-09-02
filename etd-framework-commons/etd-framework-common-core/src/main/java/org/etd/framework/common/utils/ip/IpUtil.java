package org.etd.framework.common.utils.ip;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.etd.framework.common.core.constants.HeaderConstant;

/**
 * IP 地址工具类
 * 基于 com.github.seancfoley:ipaddress 开源类库提供高效、安全的 IP 解析与校验
 *
 * @author 牛昌
 */
public final class IpUtil {

    private IpUtil() {
        // 禁止实例化
    }

    /**
     * 获取客户端真实 IP 地址
     *
     * @param request HTTP 请求对象
     * @return 客户端 IP 地址；未能识别时返回 remoteAddr
     */
    /**
     * 获取 RemoteIp 属性值
     *
     * @param request 参数 request
     * @return 处理结果
     */
    public static String getRemoteIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        for (String header : HeaderConstant.IP_HEADERS) {
            String ip = request.getHeader(header);
            if (StringUtils.isNotEmpty(ip) && !"unknown".equalsIgnoreCase(ip)) {
                int commaIndex = ip.indexOf(',');
                String realIp = commaIndex > -1 ? ip.substring(0, commaIndex).trim() : ip.trim();
                IPAddressString ipAddressString = new IPAddressString(realIp);
                if (ipAddressString.isIPAddress()) {
                    IPAddress address = ipAddressString.getAddress();
                    return address.isLoopback() ? "127.0.0.1" : address.toNormalizedString();
                }
            }
        }

        String remoteAddr = request.getRemoteAddr();
        if (StringUtils.isNotEmpty(remoteAddr)) {
            IPAddressString ipAddressString = new IPAddressString(remoteAddr.trim());
            if (ipAddressString.isIPAddress()) {
                IPAddress address = ipAddressString.getAddress();
                return address.isLoopback() ? "127.0.0.1" : address.toNormalizedString();
            }
        }
        return remoteAddr;
    }

    /**
     * 判断字符串是否是一个有效的 IP 地址（支持 IPv4 / IPv6）
     *
     * @param addr IP 地址字符串
     * @return 是否为有效 IP
     */
    /**
     * 判断 IPAddr 状态
     *
     * @param addr 参数 addr
     * @return 处理结果
     */
    public static boolean isIPAddr(String addr) {
        if (StringUtils.isBlank(addr)) {
            return false;
        }
        return new IPAddressString(addr.trim()).isIPAddress();
    }

    /**
     * 判断字符串是否是一个有效的 IPv4 地址
     *
     * @param addr IP 地址字符串
     * @return 是否为有效 IPv4
     */
    /**
     * 判断 IPv4 状态
     *
     * @param addr 参数 addr
     * @return 处理结果
     */
    public static boolean isIPv4(String addr) {
        if (StringUtils.isBlank(addr)) {
            return false;
        }
        return new IPAddressString(addr.trim()).isIPv4();
    }

    /**
     * 判断字符串是否是一个有效的 IPv6 地址
     *
     * @param addr IP 地址字符串
     * @return 是否为有效 IPv6
     */
    /**
     * 判断 IPv6 状态
     *
     * @param addr 参数 addr
     * @return 处理结果
     */
    public static boolean isIPv6(String addr) {
        if (StringUtils.isBlank(addr)) {
            return false;
        }
        return new IPAddressString(addr.trim()).isIPv6();
    }

    /**
     * 判断 IP 是否为内网/私有/环回地址
     *
     * @param addr IP 地址字符串
     * @return 是否为内网/私有/环回 IP
     */
    /**
     * 判断 InternalIp 状态
     *
     * @param addr 参数 addr
     * @return 处理结果
     */
    public static boolean isInternalIp(String addr) {
        if (StringUtils.isBlank(addr)) {
            return false;
        }
        IPAddressString ipAddressString = new IPAddressString(addr.trim());
        if (!ipAddressString.isIPAddress()) {
            return false;
        }
        IPAddress address = ipAddressString.getAddress();
        return address.isLoopback() || address.isLocal() || address.isLinkLocal();
    }
}
