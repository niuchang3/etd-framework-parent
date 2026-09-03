package org.etd.upms.dict.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.upms.dict.biz.SystemDictBizService;
import org.etd.upms.dict.controller.dto.SystemDictTypeSaveDTO;
import org.etd.upms.dict.controller.vo.SystemDictDataVO;
import org.etd.upms.dict.controller.vo.SystemDictTypeVO;
import org.etd.upms.dict.service.SystemDictDataService;
import org.etd.upms.dict.service.SystemDictTypeService;
import org.etd.upms.menu.constant.MenuPermissionCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字典类型管理 Controller 控制器入口。
 */
@Validated
@RestController
@RequestMapping("/v1/dict")
public class PublicSystemDictTypeController {

    @Autowired
    private SystemDictTypeService dictTypeService;

    @Autowired
    private SystemDictDataService dictDataService;

    @Autowired
    private SystemDictBizService dictBizService;


    /**
     * 根据字典类型编码获取启用的字典数据列表
     */
    @GetMapping("/type/code/{typeCode}/data")
    public ResultModel<List<SystemDictDataVO>> getEnabledDataList(@PathVariable String typeCode) {
        SystemDictTypeVO type = dictTypeService.selectEnabledByCode(typeCode);
        return ResultModel.success(type == null ? List.of() : dictDataService.selectEnabledByTypeId(type.getId()));
    }
}
