package org.etd.upms.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.etd.upms.user.entity.SystemUserEntity;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.Set;

@Mapper
public interface SystemUserMapper extends BaseMapper<SystemUserEntity> {

    /**
     * 按用户主组织分页查询；orgIds 为 null 时不筛选组织，空集合时不返回数据
     */
    IPage<SystemUserEntity> selectUserPage(IPage<SystemUserEntity> page,
                                           @Param("keyword") String keyword,
                                           @Param("orgIds") Set<Long> orgIds,
                                           @Param("enabled") Boolean enabled,
                                           @Param("locked") Boolean locked);

    /**
     * 登录账号在全平台唯一，因此唯一性检查必须忽略租户拦截器。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            <script>
            select count(1)
            from sys_user
            where account = #{account}
              and del_flag = 0
            <if test="excludedId != null">
              and id != #{excludedId}
            </if>
            </script>
            """)
    long selectAccountCount(@Param("account") String account, @Param("excludedId") Long excludedId);

    /**
     * 手机号与账号保持一致，在全平台范围内唯一。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            <script>
            select count(1)
            from sys_user
            where mobile = #{mobile}
              and del_flag = 0
            <if test="excludedId != null">
              and id != #{excludedId}
            </if>
            </script>
            """)
    long selectMobileCount(@Param("mobile") String mobile, @Param("excludedId") Long excludedId);
}
