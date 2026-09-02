package org.etd.framework.starter.log.converter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Logback IP 转换器（包含 IP 静态缓存，避免每次日志打印触发网络/DNS解析）
 *
 * @author Young
 * @date 2020/12/16
 */
public class LogIpConverter extends ClassicConverter {

	private static final String DEFAULT_IP = "127.0.0.1";
	private static final String HOST_ADDRESS;

	static {
		String ip;
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			ip = DEFAULT_IP;
		}
		HOST_ADDRESS = StringUtils.hasText(ip) ? ip : DEFAULT_IP;
	}

	/**
	 * 转换
	 *
	 * @param iLoggingEvent 参数 iLoggingEvent
	 * @return 处理结果
	 */
	@Override
	public String convert(ILoggingEvent iLoggingEvent) {
		return HOST_ADDRESS;
	}
}
