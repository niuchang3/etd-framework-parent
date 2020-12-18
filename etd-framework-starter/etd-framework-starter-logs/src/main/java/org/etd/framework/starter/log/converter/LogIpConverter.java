package org.etd.framework.starter.log.converter;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Young
 * @description
 * @date 2020/12/16
 */
public class LogIpConverter extends ClassicConverter {
	@Override
	public String convert(ILoggingEvent iLoggingEvent) {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {

		}
		return "0.0.0.0";
	}
}
