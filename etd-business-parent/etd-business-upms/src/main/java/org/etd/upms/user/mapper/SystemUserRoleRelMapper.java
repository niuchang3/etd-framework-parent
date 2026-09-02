package org.etd.upms.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.etd.upms.user.entity.SystemUserRoleRelEntity;
import org.etd.upms.user.controller.vo.SystemUserRoleVO;

import java.util.List;
import java.util.Set;

/**
 * 用户与角色关联关系 Mapper 接口。
 */
@Mapper
public interface SystemUserRoleRelMapper extends BaseMapper<SystemUserRoleRelEntity> {


    List<SystemUserRoleVO> selectByUserId(@Param("userId") Long userId);

    List<SystemUserRoleVO> selectAssignmentsByUserIds(@Param("userIds") Set<Long> userIds);
}
