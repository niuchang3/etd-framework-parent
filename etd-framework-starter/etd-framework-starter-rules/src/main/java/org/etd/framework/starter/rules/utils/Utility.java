package org.etd.framework.starter.rules.utils;

import org.drools.core.spi.KnowledgeHelper;

/**
 * Drools 规则引擎内置通用工具类。
 */
public class Utility {

    public static void help(final KnowledgeHelper drools, final String message) {
        System.out.println(message);
        System.out.println("rule triggered:" + drools.getRule().getName());
    }

    /**
     * helper
     *
     * @param drools 参数 drools
     * @return 处理结果
     */
    public static void helper(final KnowledgeHelper drools) {
        System.out.println("rule triggered:" + drools.getRule().getName());
    }
}