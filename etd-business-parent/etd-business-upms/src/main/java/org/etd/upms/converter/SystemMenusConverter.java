package org.etd.upms.converter;


import org.etd.upms.entity.SystemMenusEntity;
import org.etd.upms.controller.user.vo.SystemUserMenusVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface SystemMenusConverter {


    SystemUserMenusVO toUserMenu(SystemMenusEntity entity);

    List<SystemUserMenusVO> toUserMenu(List<SystemMenusEntity> entity);
}
