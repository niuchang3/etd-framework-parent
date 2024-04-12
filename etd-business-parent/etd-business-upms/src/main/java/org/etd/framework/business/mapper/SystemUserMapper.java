package org.etd.framework.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etd.framework.business.entity.SystemUserEntity;

@Mapper
public interface SystemUserMapper extends BaseMapper<SystemUserEntity> {
}
