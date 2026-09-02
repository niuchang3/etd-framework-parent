package org.etd.upms.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.etd.upms.menu.entity.SystemMenuApiRelEntity;

/**
 * 菜单与 API 关联关系 Mapper 接口。
 */
@Mapper
public interface SystemMenuApiRelMapper extends BaseMapper<SystemMenuApiRelEntity> {
}
