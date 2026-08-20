package org.etd.upms.converter;


import org.etd.upms.entity.SystemMenusEntity;
import org.etd.upms.controller.user.vo.SystemUserMenusVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface SystemMenusConverter {

    @Mapping(target = "tenantId", ignore = true)
    SystemUserMenusVO toUserMenu(SystemMenusEntity entity);

    List<SystemUserMenusVO> toUserMenu(List<SystemMenusEntity> entity);
}
