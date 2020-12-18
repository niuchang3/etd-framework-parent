package org.etd.framework.starter.cache.utils;

import org.apache.commons.lang3.StringUtils;


public class Redis {

	public final static String VAR_SPLITOR = ":";


	public static String genKey(String... keyMembers) {
		return StringUtils.join(keyMembers, VAR_SPLITOR).toUpperCase();
	}
}
