package org.etd.framework.starter.mybaits.permission.resolver;

import java.util.Set;

/**
 * 组织机构子树节点解析器 SPI 接口
 * <p>
 * 用于在框架层动态展开指定组织节点及其所有子节点的 ID 集合。
 *
 * @author 牛昌
 */
@FunctionalInterface
public interface OrgSubtreeResolver {

    /**
     * 根据组织 ID 展开获取该节点及其所有下级子孙节点的 ID 集合
     *
     * @param organizationId 组织 ID
     * @return 子树 ID 集合
     */
    Set<Long> selectSubtreeIds(Long organizationId);
}
