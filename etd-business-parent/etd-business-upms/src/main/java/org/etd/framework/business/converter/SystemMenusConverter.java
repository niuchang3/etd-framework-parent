package org.etd.framework.business.converter;


import org.etd.framework.business.entity.SystemMenusEntity;
import org.etd.framework.business.vo.SystemUserMenusVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface SystemMenusConverter {


    SystemUserMenusVO toUserMenu(SystemMenusEntity entity);

    List<SystemUserMenusVO> toUserMenu(List<SystemMenusEntity> entity);
}
