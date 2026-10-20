package org.etd.ai.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.etd.ai.model.entity.ModelEntity;
import org.etd.ai.model.mapper.result.ModelSquareModel;

/** 全局模型管理数据访问，租户和组织拦截器均明确忽略此表。 */
public interface ModelMapper extends BaseMapper<ModelEntity> {
    /**
     * 查询首页已启用模型及当前访问范围内的接入数量，不按服务商品牌聚合。
     * @param page 分页参数
     * @param providerCode 可选服务商字典值
     * @param modelType 可选模型主能力
     * @param keyword 可选名称或代码包含匹配表达式，由能力服务转义用户输入中的通配符
     * @param connected true仅已接入、false仅未接入、null全部已启用模型
     * @return 模型及连接统计分页
     */
    IPage<ModelSquareModel> selectSquareModelPage(IPage<ModelSquareModel> page,
            @Param("providerCode") String providerCode, @Param("modelType") String modelType,
            @Param("keyword") String keyword, @Param("connected") Boolean connected);
}
