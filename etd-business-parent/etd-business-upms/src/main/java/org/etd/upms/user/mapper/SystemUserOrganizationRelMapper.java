package org.etd.upms.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.etd.upms.user.entity.SystemUserOrganizationRelEntity;
import org.etd.upms.user.controller.vo.SystemUserOrganizationVO;

import java.util.List;
import java.util.Set;

/**
 * 用户与组织关联关系 Mapper 接口。
 */
@Mapper
public interface SystemUserOrganizationRelMapper extends BaseMapper<SystemUserOrganizationRelEntity> {

    List<SystemUserOrganizationVO> selectByUserIds(@Param("userIds") Set<Long> userIds);
}
