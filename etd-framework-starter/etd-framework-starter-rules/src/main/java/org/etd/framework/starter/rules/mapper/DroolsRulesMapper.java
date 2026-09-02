package org.etd.framework.starter.rules.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.etd.framework.starter.rules.entity.DroolsRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * Drools 规则定义 MyBatis Mapper 接口。
 */
@Mapper
public interface DroolsRulesMapper extends BaseMapper<DroolsRule> {

}
