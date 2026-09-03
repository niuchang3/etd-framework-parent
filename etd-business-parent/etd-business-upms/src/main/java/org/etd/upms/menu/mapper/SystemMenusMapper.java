package org.etd.upms.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.etd.upms.menu.entity.MenuPermissionGrant;
import java.util.List;
import org.etd.upms.menu.entity.SystemMenusEntity;


/**
 * 系统菜单 MyBatis 数据访问 Mapper 接口。
 */
@Mapper
public interface SystemMenusMapper extends BaseMapper<SystemMenusEntity> {
    /** 查询用户在租户开放范围内的菜单权限；管理员标记只接受服务端解析结果。 */
    List<MenuPermissionGrant> selectPermissionGrantListByUser(
            @Param("userId") Long userId, @Param("tenantId") Long tenantId,
            @Param("tenantAdmin") boolean tenantAdmin, @Param("platformAdmin") boolean platformAdmin,
            @Param("enabled") int enabled, @Param("readOnly") String readOnly,
            @Param("readWrite") String readWrite, @Param("directory") String directory);

}
