package org.etd.upms.menu.converter;


import org.etd.upms.menu.entity.SystemMenusEntity;
import org.etd.upms.menu.controller.dto.SystemMenuSaveDTO;
import org.etd.upms.menu.controller.vo.SystemMenuVO;
import org.etd.upms.user.controller.vo.SystemUserMenusVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface SystemMenusConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "dataStatus", ignore = true)
    @Mapping(target = "delFlag", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    SystemMenusEntity toEntity(SystemMenuSaveDTO dto);

    SystemMenuVO toMenuVO(SystemMenusEntity entity);

    List<SystemMenuVO> toMenuVO(List<SystemMenusEntity> entities);

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "accessLevel", ignore = true)
    SystemUserMenusVO toUserMenu(SystemMenusEntity entity);

    List<SystemUserMenusVO> toUserMenu(List<SystemMenusEntity> entity);
}
