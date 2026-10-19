package org.etd.ai.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.etd.ai.model.entity.ModelEntity;

/** 全局模型目录数据访问，租户和组织拦截器均明确忽略此表。 */
public interface ModelMapper extends BaseMapper<ModelEntity> {}
