package org.etd.framework.starter.mybaits.base;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;


/**
 * 基础Service接口
 *
 * @param <T> Entity对象
 */
public interface BaseService<T> extends IService<T> {

	/**
	 * 检查对象是否存在
	 * @param entity
	 * @return
	 */
	boolean exist(T entity);
	/**
	 * 保存
	 *
	 * @param entity
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	boolean insert(T entity);

	/**
	 * 批量保存
	 *
	 * @param entitys
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	boolean insertBatch(Collection<T> entitys);

	/**
	 * 根据主键ID进行修改
	 *
	 * @param entity 主键
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	boolean updateByPK(T entity);


	/**
	 * 根据主键ID进行修改
	 *
	 * @param entitys 主键
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	boolean updateBatchById(Collection<T> entitys);

	/**
	 * 更新或者修改
	 *
	 * @param entity
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	boolean saveOrUpdate(T entity);

	/**
	 * 删除
	 *
	 * @param id
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	boolean deleteByPk(Serializable id);

	/**
	 * 根据ids集合进行删除
	 *
	 * @param ids
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	boolean deleteByPks(Collection<? extends Serializable> ids);

	/**
	 * 根据Entity 进行删除
	 *
	 * @param entity
	 * @return
	 */
	@Transactional(rollbackFor = Exception.class)
	boolean deleteByEntity(T entity);

	/**
	 * 查询所有
	 *
	 * @return
	 */
	List<T> findAll();

	/**
	 * 根据主键ID查询
	 *
	 * @return
	 */
	T findById(Serializable id);

	/**
	 * 根据Ids查询
	 *
	 * @param ids
	 * @return
	 */
	List<T> findByIds(Collection<? extends Serializable> ids);

	/**
	 * 根据实体进行查询
	 *
	 * @param entity
	 * @return
	 */
	List<T> findByEntity(T entity);

	/**
	 * 查询唯一数据
	 *
	 * @param entity
	 * @return
	 */
	T findOneByEntity(T entity);

	/**
	 * 根据实体And 字段进行排序查询
	 *
	 * @param entity
	 * @param columns
	 * @return
	 */
	List<T> findByEntityOrderByAsc(T entity, String... columns);

	/**
	 * 根据实体And 字段进行排序查询
	 *
	 * @param entity
	 * @param columns
	 * @return
	 */
	List<T> findByEntityOrderDesc(T entity, String... columns);

	/**
	 * 排序分页查询
	 *
	 * @param page
	 * @param entity
	 * @param columns
	 * @return
	 */
	IPage<T> findPageByEntityOrderDesc(IPage page, T entity, String... columns);


	/**
	 * 根据实体信息分页查询
	 *
	 * @param entity
	 * @return
	 */
	IPage<T> findPageByEntity(IPage page, T entity);


	/**
	 * 根据实体Like查询
	 *
	 * @param entity                  ,实际查询参数
	 * @param orders                  ,排序字段
	 * @param likeFieldNames，like的字段名
	 * @return
	 */
	List<T> likeByEntityOrderByAsc(T entity, List<String> orders, String keyword, String... likeFieldNames);

	/**
	 * 分页模糊查询
	 *
	 * @param entity
	 * @param orders
	 * @param likeFieldNames
	 * @return
	 */
	IPage<T> likeByEntityOrderByAsc(IPage page, T entity, List<String> orders, String keyword, String... likeFieldNames);

	/**
	 * 根据Entity进行统计
	 *
	 * @param entity
	 * @return
	 */
	Integer countByEntity(T entity);

	/**
	 * Entity转QueryWrapper
	 *
	 * @param entity
	 * @return
	 */
	QueryWrapper entity2QueryWrapper(T entity, String... ignoreFieldNames);

	/**
	 * 使用原生进行查询
	 *
	 * @param queryWrapper
	 * @return
	 */
	List<T> findByQueryWrapper(QueryWrapper queryWrapper);

	/**
	 * 使用原生Mybatis查询
	 *
	 * @param page
	 * @param queryWrapper
	 * @return
	 */
	IPage<T> findByQueryWrapper(IPage page, QueryWrapper queryWrapper);

	/**
	 * 使用原生Mybatis查询
	 *
	 * @param page
	 * @param lambdaQueryWrapper
	 * @return
	 */
	IPage<T> findByLambdaQueryWrapper(IPage page, LambdaQueryWrapper lambdaQueryWrapper);

}
