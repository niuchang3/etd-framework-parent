package org.etd.framework.common.utils.result;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.etd.framework.common.utils.exception.code.RequestCode;

import java.io.Serializable;

/**
 * 数据统一返回模型
 *
 * @author Young
 * @description
 * @date 2020/6/23
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ResultModel<T> implements Serializable {


	/**
	 * 操作码
	 */
	private Integer code;
	/**
	 * 提供给开发人员看的信息
	 */
	private String devMessage;
	/**
	 * 需要提示用户的信息
	 */
	private String message;
	/**
	 * 请求返回数据
	 */
	private T data;
	/**
	 * 请求URL
	 */
	private String url;


	/**
	 * 操作成功，返回数据结果集
	 *
	 * @param data
	 * @param <T>
	 * @return
	 */
	public static <T> ResultModel<T> success(T data) {

		return new ResultModel(RequestCode.SUCCESS.getCode(), RequestCode.SUCCESS.getDescription(), "", data, "");
	}

	/**
	 * 操作失败
	 *
	 * @param <T>
	 * @return
	 */
	public static <T> ResultModel<T> failed(RequestCode requestCode, String message, String url) {
		return new ResultModel(requestCode.getCode(), requestCode.getDescription(), message, "", url);
	}

	/**
	 * 操作失败
	 *
	 * @param <T>
	 * @return
	 */
	public static <T> ResultModel<T> failed(RequestCode requestCode, String devMessage, String message, String url) {
		return new ResultModel(requestCode.getCode(), devMessage, message, "", url);
	}

}
