package org.etd.upms.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etd.upms.role.entity.SystemRoleOrganizationRelEntity;

/**
 * 角色与组织关联关系 Mapper 接口。
 */
@Mapper
public interface SystemRoleOrganizationRelMapper extends BaseMapper<SystemRoleOrganizationRelEntity> {
}
