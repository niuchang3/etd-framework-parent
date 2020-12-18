package org.etd.framework.starter.es.repository.impl;

import cn.hutool.core.bean.BeanUtil;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.etd.framework.starter.es.repository.BaseRepository;
import org.etd.framework.starter.es.utils.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

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
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withPageable(pageRequest).build();
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
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).withPageable(pageRequest).build();
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
		return operations.get(operations.stringIdRepresentation(id), getEntityClass(), getIndexCoordinates());
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
		BoolQueryBuilder builders = boolQueryBuilder(toTermQuery(maps));
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(builders).build();
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
		BoolQueryBuilder queryBuilder = boolQueryBuilder(toTermQuery(maps));
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(queryBuilder).build();
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
		BoolQueryBuilder queryBuilder = boolQueryBuilder(toTermQuery(maps));
		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder).withPageable(pageRequest).build();
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
		BoolQueryBuilder queryBuilder = boolQueryBuilder(toMatchQuery(map, likeFieldNames));
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(queryBuilder).build();
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
		BoolQueryBuilder queryBuilder = boolQueryBuilder(toMatchQuery(maps, likeFieldNames));

		NativeSearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder).withPageable(pageRequest).build();
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
		operations.delete(Objects.requireNonNull(operations.stringIdRepresentation(id)), getIndexCoordinates());
	}

	/**
	 * Map转QueryBuilder
	 *
	 * @param maps
	 * @return
	 */
	private List<QueryBuilder> toTermQuery(Map<String, Object> maps) {
		List<QueryBuilder> list = new ArrayList<>();
		for (Map.Entry<String, Object> entity : maps.entrySet()) {
			if (entity.getValue() instanceof String) {
				list.add(QueryBuilders.termQuery(entity.getKey() + ".keyword", entity.getValue()));
			} else {
				list.add(QueryBuilders.termQuery(entity.getKey(), entity.getValue()));
			}
		}
		return list;
	}

	/**
	 * Map转MatchQueryBuilder
	 *
	 * @param maps
	 * @return
	 */
	private List<AbstractQueryBuilder> toMatchQuery(Map<String, Object> maps, String... likeFieldNames) {
		List<AbstractQueryBuilder> list = new ArrayList<>();
		List<AbstractQueryBuilder> shouldList = new ArrayList<>();
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
				shouldList.add(QueryBuilders.matchQuery(entity.getKey(), entity.getValue()));
			} else {
				if (entity.getValue() instanceof String) {
					list.add(QueryBuilders.termQuery(entity.getKey() + ".keyword", entity.getValue()));
				} else {
					list.add(QueryBuilders.termQuery(entity.getKey(), entity.getValue()));
				}
			}
		}
		//拼接should 查询
		if (!shouldList.isEmpty()) {
			BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
			shouldList.forEach(t -> boolQueryBuilder.should(t));
			list.add(boolQueryBuilder);
		}
		return list;
	}


	/**
	 * 查询ES
	 *
	 * @param searchQuery
	 * @return
	 */
	private SearchPage<T> select(NativeSearchQuery searchQuery) {
		SearchHits<T> hits = operations.search(searchQuery, getEntityClass(), getIndexCoordinates());
		return SearchHitSupport.searchPageFor(hits, searchQuery.getPageable());
	}

	/**
	 * @param collection
	 * @return
	 */
	private BoolQueryBuilder boolQueryBuilder(Collection collection) {
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
		queryBuilder.must().addAll(collection);
		return queryBuilder;
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
		NativeSearchQuery query = new NativeSearchQueryBuilder().withQuery(matchAllQuery()).build();
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
