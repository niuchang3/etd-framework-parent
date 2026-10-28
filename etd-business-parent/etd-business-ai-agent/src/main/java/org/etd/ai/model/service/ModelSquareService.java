package org.etd.ai.model.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.etd.ai.model.mapper.ModelMapper;
import org.etd.ai.model.mapper.result.ModelSquareModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 首页模型广场只读查询能力；每个模型独立展示，连接统计遵循框架访问范围。 */
@Service
@RequiredArgsConstructor
public class ModelSquareService {
    private final ModelMapper mapper;

    /**
     * 分页获取已启用模型及接入指引，供广场Biz编排使用，不依赖平台模型维护权限。
     * @param current 从1开始的页码
     * @param size 每页数量
     * @param providerCode 可选服务商字典值
     * @param modelType 可选主能力类型
     * @param keyword 可选模型名称或调用代码关键字，按普通文本匹配
     * @param connected true仅已配置、false仅未配置、null返回全部已启用模型
     * @return 已启用模型分页；停用或删除模型均不在首页展示
     */
    public IPage<ModelSquareModel> fetchModelPage(long current, long size, String providerCode,
            String modelType, String keyword, Boolean connected) {
        Page<ModelSquareModel> page = new Page<ModelSquareModel>(current, size);
        String search = resolveSearchPattern(keyword);
        return mapper.selectSquareModelPage(page, providerCode, modelType, search, connected);
    }

    /** 将搜索文本转为包含匹配，显式转义通配符，不把用户输入解释为查询表达式。 */
    private String resolveSearchPattern(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String escaped = keyword.trim().replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
