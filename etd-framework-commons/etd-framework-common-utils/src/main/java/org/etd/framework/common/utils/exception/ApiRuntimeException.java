package org.etd.framework.common.utils.exception;

import lombok.Data;
import org.etd.framework.common.utils.exception.code.RequestCode;

/**
 * @author Young
 * @description
 * @date 2020/11/12
 */
@Data
public class ApiRuntimeException extends RuntimeException {

	private RequestCode requestCode;

	/**
	 * Constructs a new runtime exception with {@code null} as its
	 * detail message.  The cause is not initialized, and may subsequently be
	 * initialized by a call to {@link #initCause}.
	 */
	public ApiRuntimeException(RequestCode requestCode) {
		super(requestCode.getDescription());
		this.requestCode = requestCode;
	}

	/**
	 * Constructs a new runtime exception with the specified detail message.
	 * The cause is not initialized, and may subsequently be initialized by a
	 * call to {@link #initCause}.
	 *
	 * @param message the detail message. The detail message is saved for
	 *                later retrieval by the {@link #getMessage()} method.
	 */
	public ApiRuntimeException(RequestCode requestCode, String message) {
		super(message);
		this.requestCode = requestCode;
	}

}
