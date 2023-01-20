package org.etd.framework.starter.rules.utils;

import org.drools.core.spi.KnowledgeHelper;

public class Utility {

    public static void help(final KnowledgeHelper drools, final String message) {
        System.out.println(message);
        System.out.println("rule triggered:" + drools.getRule().getName());
    }

    public static void helper(final KnowledgeHelper drools) {
        System.out.println("rule triggered:" + drools.getRule().getName());
    }
}