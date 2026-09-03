package org.etd.upms.config.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.etd.framework.common.core.model.ResultModel;
import org.etd.upms.config.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 公开配置读取入口，是否匿名访问由 Security YAML 白名单决定。 */
@Validated
@RestController
@RequestMapping("/v1/config")
public class PublicConfigController {
    @Autowired
    private SystemConfigService configService;

    /** 查询允许公开读取的启用配置值。 */
    @GetMapping("/values")
    public ResultModel<Map<String, String>> enabledValues(
            @RequestParam("parameterKeys") @NotEmpty(message = "参数键不能为空")
            @Size(max = 100, message = "单次最多查询100个参数")
            List<@NotBlank(message = "参数键不能为空")
                    @Size(max = 100, message = "参数键不能超过100个字符") String> parameterKeys) {
        return ResultModel.success(configService.selectEnabledValuesByKeys(parameterKeys));
    }

}
