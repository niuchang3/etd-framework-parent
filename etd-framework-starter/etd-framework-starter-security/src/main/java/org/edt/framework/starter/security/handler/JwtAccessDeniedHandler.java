package org.edt.framework.starter.security.handler;

import org.etd.framework.common.core.exception.code.RequestCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Young
 * @description
 * @date 2020/12/29
 */
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
	/**
	 *
	 * 返回403状态码
	 * @param request               that resulted in an <code>AccessDeniedException</code>
	 * @param response              so that the user agent can be advised of the failure
	 * @param accessDeniedException that caused the invocation
	 * @throws IOException      in the event of an IOException
	 * @throws ServletException in the event of a ServletException
	 */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
		response.sendError(RequestCode.NO_URL_PERMISSION.getCode(), RequestCode.NO_URL_PERMISSION.getDescription());
	}
}
