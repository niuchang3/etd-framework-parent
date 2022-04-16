package org.etd.framework.starter.mybaits.core.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.etd.framework.starter.mybaits.core.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * 基础Service实现类
 *
 * @param <M> Mapper
 * @param <T> Entity
 */
public class BaseServiceImpl<M extends BaseMapper<T>, T extends Serializable> extends ServiceImpl<BaseMapper<T>, T> implements BaseService<T> {





	@Autowired
	protected M mapper;


	private final static String CACHE_KEY = "SYSTEM:ENTITY";

	/**
	 * 检查对象是否存在
	 *
	 * @param entity
	 * @return
	 */
	@Override
	public boolean exist(T entity) {
		return !CollectionUtils.isEmpty(findByEntity(entity));
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insert(T entity) {
		return save(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean insertBatch(Collection<T> entitys) {
		for (T entity : entitys) {
			save(entity);
		}
		return true;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean updateByPK(T entity) {
		return super.updateById(entity);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean updateBatchById(Collection<T> entitys) {
		for (T entity : entitys) {
			updateById(entity);
		}
		return true;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean saveOrUpdateBatch(Collection<T> entitys) {
		for (T entity : entitys) {
			saveOrUpdate(entity);
		}
		return true;
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean deleteByPk(Serializable id) {
		return removeById(id);
	}

	@Transactional(rollbackFor = Exception.class)
	@Override
	public boolean deleteByPks(Collection<? extends Serializable> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return true;
		}
		for (Serializable serializable : ids) {
			deleteByPk(serializable);
		}
		return true;
	}

	/**
	 * 根据Entity 进行删除
	 *
	 * @param entity
	 * @return
	 */
	@Override
	public boolean deleteByEntity(T entity) {
		Map<String, Object> map = toMap(entity);
		mapper.deleteByMap(map);
		return true;
	}

	/**
	 * 查询所有
	 *
	 * @return
	 */
	@Override
	public List<T> findAll() {
		return mapper.selectList(new QueryWrapper<>());
	}

	@Override
	public T findById(Serializable id) {
		return getById(id);
	}

	@Override
	public List<T> findByIds(Collection<? extends Serializable> ids) {
		List<T> list = new ArrayList<>();
		Class<T> mapperClass = getMapperClass();
		TableId annotation = null;
		for (Field declaredField : mapperClass.getDeclaredFields()) {
			annotation = declaredField.getAnnotation(TableId.class);
			if (!ObjectUtils.isEmpty(annotation)) {
				break;
			}
		}
		Object pk = annotation.value();
		QueryWrapper queryWrapper = new QueryWrapper();
		queryWrapper.in(pk, ids);
		return mapper.selectList(queryWrapper);
	}

	@Override
	public List<T> findByEntity(T entity) {
		Map<String, Object> map = toMap(entity);
		return mapper.selectByMap(map);
	}

	/**
	 * 查询唯一数据
	 *
	 * @param entity
	 * @return
	 */
	@Override
	public T findOneByEntity(T entity) {
		QueryWrapper<T> wrapper = entity2QueryWrapper(entity);
		return mapper.selectOne(wrapper);
	}

	/**
	 * 根据实体And 字段进行排序查询
	 *
	 * @param entity
	 * @param columns
	 * @return
	 */
	@Override
	public List<T> findByEntityOrderByAsc(T entity, String... columns) {
		QueryWrapper queryWrapper = entity2QueryWrapper(entity);
		queryWrapper.orderByAsc(columns);
		return mapper.selectList(queryWrapper);
	}

	/**
	 * 根据实体And 字段进行排序查询
	 *
	 * @param entity
	 * @param columns
	 * @return
	 */
	@Override
	public List<T> findByEntityOrderDesc(T entity, String... columns) {
		QueryWrapper queryWrapper = entity2QueryWrapper(entity);
		queryWrapper.orderByDesc(columns);
		return mapper.selectList(queryWrapper);
	}

	/**
	 * 排序分页查询
	 *
	 * @param page
	 * @param entity
	 * @param columns
	 * @return
	 */
	@Override
	public IPage<T> findPageByEntityOrderDesc(IPage page, T entity, String... columns) {
		QueryWrapper queryWrapper = entity2QueryWrapper(entity);
		queryWrapper.orderByDesc(columns);
		return mapper.selectPage(page, queryWrapper);
	}


	@Override
	public IPage<T> findPageByEntity(IPage page, T entity) {
		QueryWrapper queryWrapper = entity2QueryWrapper(entity);
		return mapper.selectPage(page, queryWrapper);
	}

	/**
	 * 根据实体Like查询
	 *
	 * @param entity         ,实际查询参数
	 * @param orders         ,排序字段
	 * @param likeFieldNames ，like的字段名
	 * @return
	 */
	@Override
	public List<T> likeByEntityOrderByAsc(T entity, List<String> orders, String keyword, String... likeFieldNames) {
		QueryWrapper queryWrapper = entity2QueryWrapper(entity);
		queryWrapper = toLike(queryWrapper, keyword, likeFieldNames);
		queryWrapper.orderByAsc(orders.toArray());
		return mapper.selectList(queryWrapper);
	}

	/**
	 * 分页模糊查询
	 *
	 * @param page
	 * @param entity
	 * @param orders
	 * @param likeFieldNames
	 * @return
	 */
	@Override
	public IPage<T> likeByEntityOrderByAsc(IPage page, T entity, List<String> orders, String keyword, String... likeFieldNames) {
		QueryWrapper queryWrapper = entity2QueryWrapper(entity);
		queryWrapper = toLike(queryWrapper, keyword, likeFieldNames);
		queryWrapper.orderByAsc(orders.toArray());
		return mapper.selectPage(page, queryWrapper);
	}

	/**
	 * 根据Entity进行统计
	 *
	 * @param entity
	 * @return
	 */
	@Override
	public Integer countByEntity(T entity) {
		QueryWrapper queryWrapper = entity2QueryWrapper(entity);
		return mapper.selectCount(queryWrapper);
	}

	/**
	 * Entity转QueryWrapper
	 *
	 * @param entity
	 * @return
	 */
	@Override
	public QueryWrapper entity2QueryWrapper(T entity, String... ignoreFieldNames) {
		QueryWrapper queryWrapper = new QueryWrapper();
		Map<String, Object> map = toMap(entity);
		if (ObjectUtils.isEmpty(ignoreFieldNames)) {
			queryWrapper.allEq(map);
			return queryWrapper;
		}
		for (String ignoreFieldName : ignoreFieldNames) {
			String FieldName = StrUtil.toUnderlineCase(ignoreFieldName);
			map.remove(FieldName);
		}
		queryWrapper.allEq(map);
		return queryWrapper;
	}

	/**
	 * 关键字查询转换
	 *
	 * @param queryWrapper
	 * @param keyword
	 * @param likeFieldNames
	 * @return
	 */
	private QueryWrapper toLike(QueryWrapper<T> queryWrapper, String keyword, String... likeFieldNames) {
		List<String> list = Arrays.asList(likeFieldNames);
		if (StringUtils.isEmpty(keyword)) {
			return queryWrapper;
		}
		queryWrapper.and(wrapper -> {
			for (int i = 0; i < list.size(); i++) {
				String fieldName = StrUtil.toUnderlineCase(list.get(i));
				wrapper.like(fieldName, keyword);
				if (i != list.size() - 1) {
					wrapper.or();
				}
			}
		});

		return queryWrapper;
	}

	@Override
	public List<T> findByQueryWrapper(QueryWrapper queryWrapper) {
		return mapper.selectList(queryWrapper);
	}

	@Override
	public IPage<T> findByLambdaQueryWrapper(IPage page, LambdaQueryWrapper lambdaQueryWrapper) {
		return mapper.selectPage(page, lambdaQueryWrapper);
	}

	@Override
	public IPage<T> findByQueryWrapper(IPage page, QueryWrapper queryWrapper) {
		return mapper.selectPage(page, queryWrapper);
	}


	/**
	 * 对象转Map
	 *
	 * @param entity
	 * @return
	 */
	public Map<String, Object> toMap(T entity) {
		return BeanUtil.beanToMap(entity, true, true);
	}

	/**
	 * 获取Mapper中泛型内容
	 *
	 * @return
	 */
	private Class<T> getMapperClass() {
		ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
		return (Class<T>) pt.getActualTypeArguments()[1];
	}
}
