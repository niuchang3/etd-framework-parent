package org.etd.framework.starter.es.repository;


import org.etd.framework.starter.es.utils.PageInfo;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * @author Young
 * @description
 * @date 2020/7/23
 */
@NoRepositoryBean
@Component
public interface BaseRepository<T, ID> extends Repository<T, ID> {


	/**
	 * 保存文档内容
	 *
	 * @param entity
	 * @return
	 */
	T insert(T entity);

	/**
	 * 批量保存
	 *
	 * @param collection
	 */
	void insertBatch(Collection<T> collection);

	/**
	 * 查询所有文档内容
	 *
	 * @return
	 */
	List<T> findAll(Sort.Order... orders);

	/**
	 * 分页查询全部数据
	 *
	 * @param pageInfo
	 * @param orders
	 * @return
	 */
	PageInfo<T> findAll(PageInfo pageInfo, Sort.Order... orders);

	/**
	 * 根据ID进行查询
	 *
	 * @param id
	 * @return
	 */
	T findById(ID id);

	/**
	 * 根据实体进行查询
	 *
	 * @param entity
	 * @return
	 */
	List<T> findByEntity(T entity);

	/**
	 * 分页查询文档信息
	 *
	 * @param entity
	 * @param orders
	 * @return
	 */
	List<T> findByEntity(T entity, Sort.Order... orders);

	/**
	 * 根据文档内容分页查询
	 *
	 * @param pageInfo
	 * @param entity
	 * @param orders
	 * @return
	 */
	PageInfo<T> findByEntity(PageInfo pageInfo, T entity, Sort.Order... orders);

	/**
	 * 模糊查询
	 *
	 * @param entity
	 * @return
	 */
	List<T> likeByEntity(T entity, String... likeFieldNames);

	/**
	 * @param entity
	 * @return
	 */
	PageInfo<T> likeByEntity(PageInfo pageInfo, T entity, Sort sort, String... likeFieldNames);

	/**
	 * 根据主键ID删除
	 *
	 * @param id
	 */
	void deleteById(ID id);

	/**
	 * 根据主键ID批量删除
	 */
	void deleteById(Collection<ID> ids);

	/**
	 * 统计当前文档总量
	 *
	 * @return
	 */
	public long count();
}
