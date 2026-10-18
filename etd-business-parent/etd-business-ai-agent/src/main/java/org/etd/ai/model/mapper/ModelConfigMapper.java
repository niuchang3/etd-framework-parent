package org.etd.ai.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.etd.ai.model.entity.ModelConfigEntity;

/** 模型配置数据访问，租户隔离由框架负责。 */
public interface ModelConfigMapper extends BaseMapper<ModelConfigEntity> {}
