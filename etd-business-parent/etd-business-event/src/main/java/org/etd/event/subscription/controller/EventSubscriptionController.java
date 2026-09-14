package org.etd.event.subscription.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.etd.framework.starter.client.core.permission.annotation.Permission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.etd.event.biz.EventManagementBizService;
import org.etd.event.constant.EventPermissionCode;
import org.etd.event.subscription.controller.dto.EventSubscriptionSaveDTO;
import org.etd.event.subscription.controller.dto.EventSubscriptionUpdateDTO;
import org.etd.event.subscription.controller.vo.EventSubscriptionVO;
import org.etd.event.subscription.service.EventSubscriptionService;
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
 * 业务事件订阅配置管理入口。
 */
@Validated
@Permission(EventPermissionCode.SUBSCRIPTION)
@RestController
@RequestMapping("/v1/event/subscriptions")
public class EventSubscriptionController {

    private final EventSubscriptionService subscriptionService;
    private final EventManagementBizService eventManagementBizService;

    public EventSubscriptionController(EventSubscriptionService subscriptionService,
                                       EventManagementBizService eventManagementBizService) {
        this.subscriptionService = subscriptionService;
        this.eventManagementBizService = eventManagementBizService;
    }

    /**
     * 分页查询订阅配置及其事件类型。
     */
    @GetMapping
    public ResultModel<IPage<EventSubscriptionVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long current,
            @RequestParam(defaultValue = "10") @Min(1) @Max(200) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long eventTypeId,
            @RequestParam(required = false) String subscriberApplication,
            @RequestParam(required = false) Boolean enabled) {
        return ResultModel.success(subscriptionService.page(current, size, keyword,
                eventTypeId, subscriberApplication, enabled));
    }

    /**
     * 查询启用订阅，供投递记录筛选器使用。
     */
    @GetMapping("/options")
    public ResultModel<List<EventSubscriptionVO>> listOptions() {
        return ResultModel.success(subscriptionService.selectEnabledList());
    }

    /**
     * 查询订阅详情及对应事件类型。
     */
    @GetMapping("/{id}")
    public ResultModel<EventSubscriptionVO> get(@PathVariable Long id) {
        return ResultModel.success(subscriptionService.fetchById(id));
    }

    /**
     * 新增业务事件订阅。
     */
    @AutoLog("新增事件订阅")
    @PostMapping
    public ResultModel<Long> save(@Valid @RequestBody EventSubscriptionSaveDTO dto) {
        return ResultModel.success(eventManagementBizService.createSubscription(dto));
    }

    /**
     * 更新业务事件订阅，订阅编码创建后不可修改。
     */
    @AutoLog("更新事件订阅")
    @PutMapping("/{id}")
    public ResultModel<Boolean> update(@PathVariable Long id,
                                       @Valid @RequestBody EventSubscriptionUpdateDTO dto) {
        return ResultModel.success(eventManagementBizService.modifySubscription(id, dto));
    }

    /**
     * 启用或禁用业务订阅。
     */
    @AutoLog("切换事件订阅状态")
    @PatchMapping("/{id}/enabled/{enabled}")
    public ResultModel<Boolean> switchEnabled(@PathVariable Long id, @PathVariable Boolean enabled) {
        return ResultModel.success(subscriptionService.switchEnabled(id, enabled));
    }

    /**
     * 逻辑删除订阅配置，历史投递快照仍保留。
     */
    @AutoLog("删除事件订阅")
    @DeleteMapping("/{id}")
    public ResultModel<Boolean> remove(@PathVariable Long id) {
        return ResultModel.success(subscriptionService.remove(id));
    }
}
