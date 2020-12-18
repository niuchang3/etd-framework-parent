package org.etd.framework.starter.mybaits.config;

import com.baomidou.mybatisplus.extension.plugins.OptimisticLockerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import org.etd.framework.starter.mybaits.handler.MetaObjectHandlerExtend;
import org.etd.framework.starter.mybaits.handler.defaults.DefaultMetaObjectHandlerExtend;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Young
 * @description
 * @date 2020/6/16
 */
@Configuration
public class MybatisPlusConfig {

	@Bean
	@ConditionalOnMissingBean(MetaObjectHandlerExtend.class)
	public DefaultMetaObjectHandlerExtend metaObjectHandlerExtend() {
		return new DefaultMetaObjectHandlerExtend();
	}

	/**
	 * 引入分页插件
	 *
	 * @return
	 */
	@Bean
	public PaginationInterceptor paginationInterceptor() {
		PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
		// 设置请求的页面大于最大页后操作， true调回到首页，false 继续请求  默认false
		// paginationInterceptor.setOverflow(false);
		// 设置最大单页限制数量，默认 500 条，-1 不受限制
		// paginationInterceptor.setLimit(500);
		// 开启 count 的 join 优化,只针对部分 left join
//		paginationInterceptor.setCountSqlParser(new JsqlParserCountOptimize(true));
		return paginationInterceptor;
	}

	/**
	 * 引入乐观锁插件
	 * 支持类型：int,Integer,long,Long,Date,Timestamp,LocalDateTime
	 *
	 * @return
	 */
	@Bean
	public OptimisticLockerInterceptor optimisticLockerInterceptor() {
		return new OptimisticLockerInterceptor();
	}

}
