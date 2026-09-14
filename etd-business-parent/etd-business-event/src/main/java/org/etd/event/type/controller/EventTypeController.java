package org.etd.event.type.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.etd.event.biz.EventManagementBizService;
import org.etd.event.constant.EventPermissionCode;
import org.etd.event.type.controller.dto.EventTypeSaveDTO;
import org.etd.event.type.controller.dto.EventTypeUpdateDTO;
import org.etd.event.type.controller.vo.EventTypeVO;
import org.etd.event.type.service.EventTypeService;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.framework.starter.log.annotation.AutoLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 事件类型配置管理入口。
 */
@Validated
@Permission(EventPermissionCode.TYPE)
@RestController
@RequestMapping("/v1/event/types")
public class EventTypeController {

    private final EventTypeService eventTypeService;
    private final EventManagementBizService eventManagementBizService;

    public EventTypeController(EventTypeService eventTypeService,
                               EventManagementBizService eventManagementBizService) {
        this.eventTypeService = eventTypeService;
        this.eventManagementBizService = eventManagementBizService;
    }

    /**
     * 分页查询事件类型。
     */
    @GetMapping
    public ResultModel<IPage<EventTypeVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sourceApplication,
            @RequestParam(required = false) Boolean enabled) {
        return ResultModel.success(eventTypeService.page(current, size, keyword, sourceApplication, enabled));
    }

    /**
     * 查询启用的事件类型，供订阅表单选择。
     */
    @GetMapping("/options")
    public ResultModel<List<EventTypeVO>> listOptions() {
        return ResultModel.success(eventTypeService.selectEnabledList());
    }

    /**
     * 查询事件类型详情。
     */
    @GetMapping("/{id}")
    public ResultModel<EventTypeVO> get(@PathVariable Long id) {
        return ResultModel.success(eventTypeService.fetchById(id));
    }

    /**
     * 新增事件类型，事件类型编码创建后不可修改。
     */
    @AutoLog("新增事件类型")
    @PostMapping
    public ResultModel<Long> save(@Valid @RequestBody EventTypeSaveDTO dto) {
        return ResultModel.success(eventTypeService.create(dto));
    }

    /**
     * 更新事件类型名称、版本和说明。
     */
    @AutoLog("更新事件类型")
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id,
                                       @Valid @RequestBody EventTypeUpdateDTO dto) {
        return ResultModel.success(eventTypeService.modify(id, dto));
    }

    /**
     * 启用或禁用事件类型。
     */
    @AutoLog("切换事件类型状态")
    @PatchMapping("/{id}/enabled/{enabled}")
    public ResultModel<Boolean> switchEnabled(@PathVariable Long id, @PathVariable Boolean enabled) {
        return ResultModel.success(eventTypeService.switchEnabled(id, enabled));
    }

    /**
     * 删除未被订阅引用的事件类型。
     */
    @AutoLog("删除事件类型")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long id) {
        return ResultModel.success(eventManagementBizService.removeEventType(id));
    }
}
