package org.etd.upms.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etd.upms.role.entity.SystemRoleMenuRelEntity;

/**
 * 角色与菜单关联关系 Mapper 接口。
 */
@Mapper
public interface SystemRoleMenuRelMapper extends BaseMapper<SystemRoleMenuRelEntity> {
}
