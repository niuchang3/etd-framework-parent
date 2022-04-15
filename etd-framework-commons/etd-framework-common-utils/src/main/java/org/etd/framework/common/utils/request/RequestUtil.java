package org.etd.framework.common.utils.request;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Request工具类
 *
 * @author 牛昌
 */
public class RequestUtil {

    /**
     * 获得客户端ip
     */
    public static String getRemoteIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    /**
     * 判断是否为搜索引擎
     */
    public static boolean isRobot(HttpServletRequest req) {
        String ua = req.getHeader("user-agent");
        if (StringUtils.isBlank(ua)) {
            return false;
        }
        return ua.contains("Baiduspider") || ua.contains("Googlebot") || ua.contains("sogou") || ua.contains("sina") || ua.contains("iaskspider") || ua.contains("ia_archiver") || ua.contains("Sosospider") || ua.contains("YoudaoBot") || ua.contains("yahoo") || ua.contains("yodao") || ua.contains("MSNBot") || ua.contains("spider") || ua.contains("Twiceler") || ua.contains("Sosoimagespider") || ua.contains("naver.com/robots") || ua.contains("Nutch") || ua.contains("spider");
    }

    /**
     * 获取COOKIE
     */
    public static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie ck : cookies) {
            if (StringUtils.equalsIgnoreCase(name, ck.getName())) {
                return ck;
            }
        }
        return null;
    }

    /**
     * 获取COOKIE
     */
    public static String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie ck : cookies) {
            if (StringUtils.equalsIgnoreCase(name, ck.getName())) {
                return ck.getValue();
            }
        }
        return null;
    }

    /**
     * 设置COOKIE
     */
    public static void setCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
                                 int maxAge) {
        setCookie(request, response, name, value, maxAge, true);
    }

    /**
     * 设置COOKIE
     */
    public static void setCookie(HttpServletRequest request, HttpServletResponse response, String name, String value,
                                 int maxAge, boolean allSubDomain) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        if (allSubDomain) {
            String serverName = request.getServerName();
            String domain = getDomainOfServerName(serverName);
            if (domain != null && domain.indexOf('.') != -1) {
                cookie.setDomain('.' + domain);
            }
        }
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name,
                                    boolean allSubDomain) {
        setCookie(request, response, name, "", 0, allSubDomain);
    }

    /**
     * 获取用户访问URL中的根域名 例如: www.dlog.cn -> dlog.cn
     */
    public static String getDomainOfServerName(String host) {
        if (isIPAddr(host)) {
            return null;
        }
        String[] names = StringUtils.split(host, '.');
        int len = names.length;
        if (len == 1) {
            return null;
        }
        if (len == 3) {
            return makeup(names[len - 2], names[len - 1]);
        }
        if (len > 3) {
            String dp = names[len - 2];
            if ("com".equalsIgnoreCase(dp) || "gov".equalsIgnoreCase(dp) || "net".equalsIgnoreCase(dp)
                    || "edu".equalsIgnoreCase(dp) || "org".equalsIgnoreCase(dp)) {
                return makeup(names[len - 3], names[len - 2], names[len - 1]);
            } else {
                return makeup(names[len - 2], names[len - 1]);
            }
        }
        return host;
    }

    /**
     * 判断字符串是否是一个IP地址
     */
    public static boolean isIPAddr(String addr) {
        if (StringUtils.isEmpty(addr)) {
            return false;
        }
        String[] ips = StringUtils.split(addr, '.');
        if (ips.length != 4) {
            return false;
        }
        try {
            int ipa = Integer.parseInt(ips[0]);
            int ipb = Integer.parseInt(ips[1]);
            int ipc = Integer.parseInt(ips[2]);
            int ipd = Integer.parseInt(ips[3]);
            return ipa >= 0 && ipa <= 255 && ipb >= 0 && ipb <= 255 && ipc >= 0 && ipc <= 255 && ipd >= 0 && ipd <= 255;
        } catch (Exception ignored) {
        }
        return false;
    }

    private static String makeup(String... ps) {
        StringBuilder s = new StringBuilder();
        for (int idx = 0; idx < ps.length; idx++) {
            if (idx > 0) {
                s.append('.');
            }
            s.append(ps[idx]);
        }
        return s.toString();
    }

    /**
     * 获取HTTP端口
     */
    public static int getHttpPort(HttpServletRequest req) {
        try {
            return new URL(req.getRequestURL().toString()).getPort();
        } catch (MalformedURLException excp) {
            return 80;
        }
    }

    /**
     * 获取浏览器提交的整形参数
     */
    public static int getParam(HttpServletRequest req, String param, int defaultValue) {
        return NumberUtils.toInt(req.getParameter(param), defaultValue);
    }

    /**
     * 获取浏览器提交的整形参数
     */
    public static long getParam(HttpServletRequest req, String param, long defaultValue) {
        return NumberUtils.toLong(req.getParameter(param), defaultValue);
    }


    /**
     * 获取浏览器提交的字符串参
     */
    public static String getParam(HttpServletRequest req, String param, String defaultValue) {
        String value = req.getParameter(param);
        return (StringUtils.isEmpty(value)) ? defaultValue : value;
    }
}