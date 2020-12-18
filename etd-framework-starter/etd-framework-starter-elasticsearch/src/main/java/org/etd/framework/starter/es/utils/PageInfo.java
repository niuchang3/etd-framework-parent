package org.etd.framework.starter.es.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.ObjectUtils;

import java.util.Collections;
import java.util.List;

/**
 * @author Young
 * @description
 * @date 2020/7/23
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo<T> {
	/**
	 * 数据总量
	 */
	protected Integer total = 0;
	/**
	 * 当前页码
	 */
	protected Integer current = 0;
	/**
	 * 每页大小
	 */
	protected Integer size = 10;


	/**
	 * 数据
	 */
	protected List<T> records = Collections.emptyList();

	/**
	 * 初始化分页对象
	 * @param current
	 * @param size
	 */
	public PageInfo(Integer current, Integer size) {
		this.current = current;
		this.size = size;
	}


	/**
	 * 转Es分页查询信息
	 * @param current
	 * @param size
	 * @param sort
	 * @return
	 */
	public static PageRequest toPageRequest(Integer current, Integer size, Sort sort) {
		if (!ObjectUtils.isEmpty(sort)) {
			return PageRequest.of(current, size, sort);
		}
		return PageRequest.of(current, size);
	}

	/**
	 * 转Es分页查询信息
	 * @param sort
	 * @return
	 */
	public PageRequest toPageRequest(Sort sort) {
		if (!ObjectUtils.isEmpty(sort)) {
			return PageRequest.of(current-1, size, sort);
		}
		return PageRequest.of(current-1, size);
	}

}
