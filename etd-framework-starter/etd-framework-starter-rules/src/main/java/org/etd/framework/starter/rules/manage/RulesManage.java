package org.etd.framework.starter.rules.manage;

import cn.hutool.core.io.IoUtil;
import org.etd.framework.starter.rules.entity.DroolsRule;
import org.etd.framework.starter.rules.properties.RulesProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kie.builder.impl.KieContainerImpl;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
public class RulesManage {

    protected KieServices kieServices;

    protected KieFileSystem kieFileSystem;

    protected KieModuleModel kieModuleModel;


    protected KieContainer kieContainer;

    protected RulesProperties properties;

    private static final Pattern p = Pattern.compile("package(.*);");

    public RulesManage(KieServices kieServices, KieFileSystem kieFileSystem, KieModuleModel kieModuleModel, RulesProperties properties) {
        this.kieServices = kieServices;
        this.kieFileSystem = kieFileSystem;
        this.kieModuleModel = kieModuleModel;
        this.properties = properties;
    }


    @PostConstruct
    public void init() throws IOException {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        Resource[] files = resourcePatternResolver.getResources("classpath*:" + properties.getPath());

        for (Resource file : files) {
            String fileName = file.getFilename().substring(0, file.getFilename().lastIndexOf("."));
            String content = IoUtil.read(file.getInputStream(), Charset.forName("UTF-8"));
            Matcher matcher = p.matcher(content);
            matcher.find();
            String packageStr = matcher.group(1);
            DroolsRule rule = new DroolsRule();
            rule.setId(fileName);
            rule.setKieBaseName(fileName);
            rule.setKiePackageName(packageStr);
            rule.setRuleContent(content);
            rule.validate();
            createKieBaseName(rule);
            writeRules(rule);
            if (kieModuleModel != null) {
                String kmoduleXml = kieModuleModel.toXML();
                log.info("加载kmodule.xml:[\n{}]", kmoduleXml);
                kieFileSystem.writeKModuleXML(kmoduleXml);
            }
        }
        buildKieContainer();
    }


    @PreDestroy
    public void destroy() {
        if (null != kieContainer) {
            kieContainer.dispose();
        }
    }

    /**
     * 判断是否存在kieBase
     *
     * @param kieBaseName
     * @return
     */
    private boolean existsKieBase(String kieBaseName) {
        if (null == kieContainer) {
            return false;
        }
        Collection<String> kieBaseNames = kieContainer.getKieBaseNames();
        if (kieBaseNames.contains(kieBaseName)) {
            return true;
        }
        log.info("需要创建KieBase:{}", kieBaseName);
        return false;
    }

    /**
     * 添加规则
     *
     * @param droolsRule
     */
    public void addRules(DroolsRule droolsRule) {
        droolsRule.validate();
        createKieBaseName(droolsRule);
        writeRules(droolsRule);
        if (kieModuleModel != null) {
            String kmoduleXml = kieModuleModel.toXML();
            log.info("加载kmodule.xml:[\n{}]", kmoduleXml);
            kieFileSystem.writeKModuleXML(kmoduleXml);
        }
        buildKieContainer();
    }

    protected void buildKieContainer() {
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        // 通过KieBuilder构建KieModule下所有的KieBase
        kieBuilder.buildAll();
        // 获取构建过程中的结果
        Results results = kieBuilder.getResults();
        // 获取错误信息
        List<Message> messages = results.getMessages(Message.Level.ERROR);
        if (null != messages && !messages.isEmpty()) {
            for (Message message : messages) {
                log.error(message.getText());
            }
            throw new RuntimeException("加载规则出现异常");
        }
        // KieContainer只有第一次时才需要创建，之后就是使用这个
        if (null == kieContainer) {
            kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
        } else {
            // 实现动态更新
            ((KieContainerImpl) kieContainer).updateToKieModule((InternalKieModule) kieBuilder.getKieModule());
        }
    }

    /**
     * 删除规则
     *
     * @param kieBaseName
     * @param packageName
     * @param ruleName
     */
    public void deleteDroolsRule(String kieBaseName, String packageName, String ruleName) {
        if (existsKieBase(kieBaseName)) {
            KieBase kieBase = kieContainer.getKieBase(kieBaseName);
            kieBase.removeRule(packageName, ruleName);
            log.info("删除kieBase:[{}]包:[{}]下的规则:[{}]", kieBaseName, packageName, ruleName);
        }
    }


    /**
     * 触发规则
     */
    public void fireRule(String kieBaseName, Object param) {
        if (ObjectUtils.isEmpty(kieContainer)) {
            kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
        }
        // 创建kieSession
        System.setProperty("drools.dateformat", "yyyy-MM-dd HH:mm:ss");
        KieSession kieSession = kieContainer.newKieSession(kieBaseName + "-session");
        kieSession.insert(param);
        kieSession.fireAllRules();
        kieSession.dispose();
    }

    /**
     * 创建
     *
     * @param droolsRule
     */
    private KieBaseModel createKieBaseName(DroolsRule droolsRule) {
        if (existsKieBase(droolsRule.getKieBaseName())) {
            KieBaseModel kieBaseModel = kieModuleModel.getKieBaseModels().get(droolsRule.getKieBaseName());
            // 获取到packages
            List<String> packages = kieBaseModel.getPackages();
            if (!packages.contains(droolsRule.getKiePackageName())) {
                kieBaseModel.addPackage(droolsRule.getKiePackageName());
                log.info("kieBase:{}添加一个新的包:{}", droolsRule.getKieBaseName(), droolsRule.getKiePackageName());
            }
            return kieBaseModel;
        }

        KieBaseModel kieBaseModel = kieModuleModel.newKieBaseModel(droolsRule.getKieBaseName());
        // 不是默认的kieBase
        kieBaseModel.setDefault(false);
        // 设置该KieBase需要加载的包路径
        kieBaseModel.addPackage(droolsRule.getKiePackageName());
        // 设置kieSession
        kieBaseModel.newKieSessionModel(droolsRule.getKieBaseName() + "-session")
                // 不是默认session
                .setDefault(false);
        return kieBaseModel;
    }

    /**
     * 写入规则
     */
    protected void writeRules(DroolsRule droolsRule) {
        String file = "src/main/resources/" + droolsRule.getKiePackageName() + "/" + droolsRule.getId() + ".drl";
        log.info("加载虚拟规则文件:{}", file);
        kieFileSystem.write(file, droolsRule.getRuleContent());
    }
}
