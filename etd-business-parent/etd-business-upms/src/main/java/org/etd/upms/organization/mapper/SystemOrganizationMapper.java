package org.etd.upms.organization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.etd.upms.organization.entity.SystemOrganizationEntity;

import java.util.Collection;
import java.util.List;

/**
 * 组织机构 MyBatis Mapper 接口。
 */
@Mapper
public interface SystemOrganizationMapper extends BaseMapper<SystemOrganizationEntity> {

    @Select("select count(1) from sys_user_org_rel where org_id = #{orgId} and del_flag = 0")
    long selectUserReferenceCount(@Param("orgId") Long orgId);

    @Select("select count(1) from sys_role_org_rel where org_id = #{orgId} and del_flag = 0")
    long selectRoleReferenceCount(@Param("orgId") Long orgId);

    /**
     * 根据用户 ID 联表查询组织机构列表
     */
    List<SystemOrganizationEntity> selectListByUserId(@Param("userId") Long userId, @Param("enabled") Boolean enabled);

    /**
     * 根据组织 ID 集合查询组织机构列表
     */
    List<SystemOrganizationEntity> selectListByIds(@Param("ids") Collection<Long> ids, @Param("enabled") Boolean enabled);
}
