package org.etd.framework.starter.es.repository.impl;

import cn.hutool.core.bean.BeanUtil;
import org.etd.framework.starter.es.repository.BaseRepository;
import org.etd.framework.starter.es.utils.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Young
 * @description
 * @date 2020/7/23
 */
@Component
public abstract class BaseRepositoryImpl<T, ID> implements BaseRepository<T, ID> {

	/**
	 * ES操作实体类
	 */
	@Autowired
	protected ElasticsearchOperations operations;

	/**
	 * 获取当前文档索引
	 *
	 * @return
	 */
	private IndexCoordinates getIndexCoordinates() {
		return operations.getIndexCoordinatesFor(getEntityClass());
	}

	/**
	 * 获取实现类泛型类型
	 *
	 * @return
	 */
	private Class<T> getEntityClass() {
		ParameterizedType pt = (ParameterizedType) this.getClass().getGenericSuperclass();
		return (Class<T>) pt.getActualTypeArguments()[0];
	}


	/**
	 * 保存文档内容
	 *
	 * @param entity
	 * @return
	 */
	@Override
	public T insert(T entity) {
		return operations.save(entity);
	}

	/**
	 * 批量保存
	 *
	 * @param collection
	 */
	@Override
	public void insertBatch(Collection<T> collection) {
		for (T entity : collection) {
			insert(entity);
		}
	}

	/**
	 * 查询所有文档内容
	 *
	 * @return
	 */
	@Override
	public List<T> findAll(Sort.Order... orders) {
		int itemCount = (int) this.count();
		if (itemCount == 0) {
			return Collections.emptyList();
		}
		PageRequest pageRequest = PageInfo.toPageRequest(0, Math.max(1, itemCount), getSort(orders));
		CriteriaQuery query = CriteriaQuery.builder(new Criteria()).withPageable(pageRequest).build();
		SearchPage<T> page = select(query);
		return toList(page);
	}

	/**
	 * 分页查询全部数据
	 *
	 * @param pageInfo
	 * @param orders
	 * @return
	 */
	@Override
	public PageInfo<T> findAll(PageInfo pageInfo, Sort.Order... orders) {
		PageRequest pageRequest = pageInfo.toPageRequest(getSort(orders));
		CriteriaQuery query = CriteriaQuery.builder(new Criteria()).withPageable(pageRequest).build();
		SearchPage<T> page = select(query);
		pageInfo.setTotal(Long.valueOf(page.getTotalElements()).intValue());
		pageInfo.setRecords(toList(page));
		return pageInfo;
	}

	/**
	 * 根据实体类进行查询
	 *
	 * @param id
	 * @return
	 */
	@Override
	public T findById(ID id) {
		return operations.get(String.valueOf(id), getEntityClass(), getIndexCoordinates());
	}

	/**
	 * 根据实体进行查询
	 *
	 * @param entity
	 * @return
	 */
	@Override
	public List<T> findByEntity(T entity) {
		Map<String, Object> maps = toMap(entity);
		CriteriaQuery query = CriteriaQuery.builder(toTermCriteria(maps)).build();
		SearchPage<T> page = select(query);
		return toList(page);
	}

	/**
	 * 分页查询文档信息
	 *
	 * @param entity
	 * @param orders
	 * @return
	 */
	@Override
	public List<T> findByEntity(T entity, Sort.Order... orders) {
		Map<String, Object> maps = toMap(entity);
		CriteriaQuery query = CriteriaQuery.builder(toTermCriteria(maps)).withSort(getSort(orders)).build();
		SearchPage<T> page = select(query);
		return toList(page);
	}

	/**
	 * 根据文档内容分页查询
	 *
	 * @param pageInfo
	 * @param entity
	 * @param orders
	 * @return
	 */
	@Override
	public PageInfo<T> findByEntity(PageInfo pageInfo, T entity, Sort.Order... orders) {
		PageRequest pageRequest = pageInfo.toPageRequest(getSort(orders));
		Map<String, Object> maps = toMap(entity);
		CriteriaQuery searchQuery = CriteriaQuery.builder(toTermCriteria(maps)).withPageable(pageRequest).build();
		SearchPage<T> page = select(searchQuery);
		pageInfo.setTotal((int) page.getTotalElements());
		pageInfo.setRecords(page.getContent().stream().map(SearchHit::getContent).collect(Collectors.toList()));
		return pageInfo;
	}

	/**
	 * 模糊查询
	 *
	 * @param entity
	 * @return
	 */
	@Override
	public List<T> likeByEntity(T entity, String... likeFieldNames) {
		Map<String, Object> map = toMap(entity);
		CriteriaQuery query = CriteriaQuery.builder(toMatchCriteria(map, likeFieldNames)).build();
		SearchPage<T> page = select(query);
		return toList(page);
	}

	/**
	 * @param pageInfo
	 * @param entity
	 * @return
	 */
	@Override
	public PageInfo<T> likeByEntity(PageInfo pageInfo, T entity, Sort sort, String... likeFieldNames) {
		PageRequest pageRequest = pageInfo.toPageRequest(sort);
		Map<String, Object> maps = toMap(entity);
		CriteriaQuery searchQuery = CriteriaQuery.builder(toMatchCriteria(maps, likeFieldNames)).withPageable(pageRequest).build();
		SearchPage<T> page = select(searchQuery);
		pageInfo.setTotal((int) page.getTotalElements());
		pageInfo.setRecords(page.getContent().stream().map(SearchHit::getContent).collect(Collectors.toList()));
		return pageInfo;
	}

	/***
	 * 根据主键ID删除
	 * @param id
	 */
	@Override
	public void deleteById(ID id) {
		delete(id);
		operations.indexOps(getEntityClass()).refresh();
	}

	/**
	 * 根据主键ID批量删除
	 *
	 * @param ids
	 */
	@Override
	public void deleteById(Collection<ID> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return;
		}
		for (ID id : ids) {
			delete(id);
		}
		operations.indexOps(getEntityClass()).refresh();
	}


	/***
	 * 根据主键ID删除
	 * @param id
	 */
	private void delete(ID id) {
		operations.delete(Objects.requireNonNull(String.valueOf(id)), getIndexCoordinates());
	}

	/**
	 * Map转Criteria
	 *
	 * @param maps
	 * @return
	 */
	private Criteria toTermCriteria(Map<String, Object> maps) {
		Criteria criteria = new Criteria();
		for (Map.Entry<String, Object> entity : maps.entrySet()) {
			if (ObjectUtils.isEmpty(entity.getValue())) {
				continue;
			}
			if (entity.getValue() instanceof String) {
				criteria.and(entity.getKey() + ".keyword").is(entity.getValue());
			} else {
				criteria.and(entity.getKey()).is(entity.getValue());
			}
		}
		return criteria;
	}

	/**
	 * Map转Match Criteria
	 *
	 * @param maps
	 * @return
	 */
	private Criteria toMatchCriteria(Map<String, Object> maps, String... likeFieldNames) {
		Criteria criteria = new Criteria();
		List<String> fieldNames = new ArrayList<>();
		if (likeFieldNames != null) {
			fieldNames = Arrays.asList(likeFieldNames);
		}
		for (Map.Entry<String, Object> entity : maps.entrySet()) {
			//如果是空集合 跳过
			if (entity.getValue() instanceof Collection) {
				if (CollectionUtils.isEmpty((Collection) entity.getValue())) {
					continue;
				}
			}
			if (fieldNames.contains(entity.getKey())) {
				criteria.and(entity.getKey()).matches(entity.getValue());
			} else {
				if (entity.getValue() instanceof String) {
					criteria.and(entity.getKey() + ".keyword").is(entity.getValue());
				} else {
					criteria.and(entity.getKey()).is(entity.getValue());
				}
			}
		}
		return criteria;
	}


	/**
	 * 查询ES
	 *
	 * @param searchQuery
	 * @return
	 */
	private SearchPage<T> select(Query searchQuery) {
		SearchHits<T> hits = operations.search(searchQuery, getEntityClass(), getIndexCoordinates());
		return SearchHitSupport.searchPageFor(hits, searchQuery.getPageable());
	}

	/**
	 * 封装排序
	 *
	 * @param orders
	 * @return
	 */
	private Sort getSort(Sort.Order... orders) {
		if (ObjectUtils.isEmpty(orders)) {
			return null;
		}
		return Sort.by(orders);
	}


	@Override
	public long count() {
		CriteriaQuery query = CriteriaQuery.builder(new Criteria()).build();
		return operations.count(query, getEntityClass(), getIndexCoordinates());
	}

	/**
	 * 将类型转换为Map
	 *
	 * @param t
	 * @return
	 */
	private Map<String, Object> toMap(T t) {
		return BeanUtil.beanToMap(t, false, true);
	}


	/**
	 * ES对象转ArrayList集合
	 *
	 * @param datas
	 * @return
	 */
	private List<T> toList(SearchPage<T> datas) {
		return datas.getContent().stream().map(SearchHit::getContent).collect(Collectors.toList());
	}
}
