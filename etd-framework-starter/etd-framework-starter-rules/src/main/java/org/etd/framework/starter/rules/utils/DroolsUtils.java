package org.etd.framework.starter.rules.utils;

import org.drools.core.impl.InternalKnowledgeBase;
import org.drools.core.impl.KnowledgeBaseFactory;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class DroolsUtils {


    /**
     * 获取 Session 属性值
     *
     * @param drools 参数 drools
     * @return 处理结果
     */
    public static StatelessKieSession getSession(String drools) {
        KnowledgeBuilder kb = KnowledgeBuilderFactory.newKnowledgeBuilder();
        if (kb.hasErrors()) {
            throw new RuntimeException("Drools 语法错误。");
        }
        InternalKnowledgeBase kBase = KnowledgeBaseFactory.newKnowledgeBase();
        kBase.addPackages(kb.getKnowledgePackages());
        StatelessKieSession kieSession = kBase.newStatelessKieSession();
        return kieSession;
    }

    /**
     * valida Drools
     *
     * @param drools 参数 drools
     * @return 处理结果
     */
    public static void validaDrools(String drools) {
        KnowledgeBuilder kb = getKnowledgeBuilder(drools);
        if (kb.hasErrors()) {
            throw new RuntimeException("Drools 语法错误。");
        }
    }

    /**
     * 获取知识库
     *
     * @param drools
     * @return
     */
    private static KnowledgeBuilder getKnowledgeBuilder(String drools) {
        KnowledgeBuilder kb = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kb.add(ResourceFactory.newByteArrayResource(drools.getBytes(StandardCharsets.UTF_8)), ResourceType.DRL);
        return kb;
    }

    /**
     * 触发规则
     *
     * @param drools
     * @param obj
     * @param <T>
     */
    /**
     * trigger Rule
     *
     * @param drools 参数 drools
     * @param obj 参数 obj
     * @param Map<String 参数 Map<String
     * @param global 参数 global
     * @return 处理结果
     */
    public static <T> void triggerRule(String drools, T obj, Map<String, Object> global) {
        if (ObjectUtils.isEmpty(global)) {
            triggerRule(drools, obj);
            return;
        }
        StatelessKieSession session = getSession(drools);
        global.forEach((key, value) -> {
            session.setGlobal(key, value);
        });
        session.execute(obj);
    }

    /**
     * 触发规则
     *
     * @param drools
     * @param obj
     * @param <T>
     */
    /**
     * trigger Rule
     *
     * @param drools 参数 drools
     * @param obj 参数 obj
     * @return 处理结果
     */
    public static <T, G> void triggerRule(String drools, T obj) {
        StatelessKieSession session = getSession(drools);
        session.execute(obj);
    }
}
