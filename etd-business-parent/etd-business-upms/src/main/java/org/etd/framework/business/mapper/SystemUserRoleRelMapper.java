package org.etd.framework.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.etd.framework.business.entity.SystemUserRoleRelEntity;
import org.etd.framework.business.vo.SystemUserRoleVO;

import java.util.List;

@Mapper
public interface SystemUserRoleRelMapper extends BaseMapper<SystemUserRoleRelEntity> {


    List<SystemUserRoleVO> selectByUserId(@Param("userId") Long userId);
}
