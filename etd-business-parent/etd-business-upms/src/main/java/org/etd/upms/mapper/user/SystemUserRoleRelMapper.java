package org.etd.upms.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.etd.upms.entity.SystemUserRoleRelEntity;
import org.etd.upms.controller.user.vo.SystemUserRoleVO;

import java.util.List;

@Mapper
public interface SystemUserRoleRelMapper extends BaseMapper<SystemUserRoleRelEntity> {


    List<SystemUserRoleVO> selectByUserId(@Param("userId") Long userId);
}
