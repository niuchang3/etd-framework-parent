package org.etd.framework.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.etd.framework.business.entity.SystemUserRoleRelEntity;
import org.etd.framework.business.vo.UserRoleVo;

import java.util.List;

@Mapper
public interface SystemUserRoleRelMapper extends BaseMapper<SystemUserRoleRelEntity> {



    List<UserRoleVo> selectByUserId(@Param("userId")Long userId);
}
