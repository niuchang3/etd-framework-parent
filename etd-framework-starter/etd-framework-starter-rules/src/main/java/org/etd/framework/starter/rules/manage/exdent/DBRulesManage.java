package org.etd.framework.starter.rules.manage.exdent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.etd.framework.common.core.constants.RequestCodeConstant;
import org.etd.framework.common.core.exception.ApiRuntimeException;
import org.etd.framework.starter.rules.entity.DroolsRule;
import org.etd.framework.starter.rules.entity.DroolsRuleNames;
import org.etd.framework.starter.rules.manage.RulesManage;
import org.etd.framework.starter.rules.mapper.DroolsRulesMapper;
import org.etd.framework.starter.rules.mapper.DroolsRulesNamesMapper;
import org.etd.framework.starter.rules.properties.RulesProperties;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.model.KieModuleModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DBRulesManage extends RulesManage {

    @Autowired
    private DroolsRulesMapper droolsRulesMapper;

    @Autowired
    private DroolsRulesNamesMapper droolsRulesNamesMapper;

    public static final Pattern p = Pattern.compile("rule.*\"(.*)\"([\\s\\S]*?)end");

    public DBRulesManage(KieServices kieServices, KieFileSystem kieFileSystem, KieModuleModel kieModuleModel, RulesProperties properties) {
        super(kieServices, kieFileSystem, kieModuleModel, properties);
    }

    @Override
    @PostConstruct
    public void init() throws IOException {
        super.init();
        List<DroolsRule> droolsRules = droolsRulesMapper.selectByMap(new HashMap<>());
        for (DroolsRule droolsRule : droolsRules) {
            super.addRules(droolsRule);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void addRules(DroolsRule droolsRule) {
        List<String> names = extractRuleName(droolsRule);
        if (!ObjectUtils.isEmpty(droolsRule.getId())) {
            droolsRulesMapper.updateById(droolsRule);
            insertRuleNames(droolsRule.getId(), names);
            super.addRules(droolsRule);
            return;
        }
        checkRuleNameExist(names);
        if (exist(droolsRule.getKieBaseName())) {
            throw new ApiRuntimeException(RequestCodeConstant.VALIDATE_ERROR, "已存在" + droolsRule.getKieBaseName() + "请勿重复添加");
        }
        droolsRule.setCreatedTime(new Date());
        droolsRulesMapper.insert(droolsRule);
        insertRuleNames(droolsRule.getId(), names);
        super.addRules(droolsRule);
    }

    private void insertRuleNames(String droolsRuleId, List<String> names) {
        LambdaQueryWrapper<DroolsRuleNames> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DroolsRuleNames::getDroolsRuleId, droolsRuleId);
        droolsRulesNamesMapper.delete(queryWrapper);
        for (String name : names) {
            DroolsRuleNames ruleNames = new DroolsRuleNames();
            ruleNames.setDroolsRuleId(droolsRuleId);
            ruleNames.setRuleName(name);
            droolsRulesNamesMapper.insert(ruleNames);
        }
    }

    private boolean exist(String kieBaseName) {
        LambdaQueryWrapper<DroolsRule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DroolsRule::getKieBaseName, kieBaseName);
        List<DroolsRule> droolsRules = droolsRulesMapper.selectList(queryWrapper);
        return !ObjectUtils.isEmpty(droolsRules);
    }

    private void checkRuleNameExist(List<String> names) {
        LambdaQueryWrapper<DroolsRuleNames> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(DroolsRuleNames::getRuleName, names);
        List<DroolsRuleNames> droolsRuleNames = droolsRulesNamesMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(droolsRuleNames)) {
            return;
        }
        throw new ApiRuntimeException(RequestCodeConstant.VALIDATE_ERROR, droolsRuleNames.get(0).getRuleName() + ",规则已存在,请勿重复添加");
    }

    private List<String> extractRuleName(DroolsRule droolsRule) {
        Matcher matcher = p.matcher(droolsRule.getRuleContent());
        List<String> rulesNames = new ArrayList<>();
        while (matcher.find()) {
            rulesNames.add(matcher.group(1));
        }
        if (ObjectUtils.isEmpty(rulesNames)) {
            throw new ApiRuntimeException(RequestCodeConstant.VALIDATE_ERROR, "Drools语法错误,规则必须包含 rule  end 体");
        }
        return rulesNames;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteDroolsRule(String kieBaseName, String packageName, String ruleName) {
        LambdaQueryWrapper<DroolsRule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DroolsRule::getKieBaseName, kieBaseName);
        DroolsRule droolsRules = droolsRulesMapper.selectOne(queryWrapper);


        LambdaQueryWrapper<DroolsRuleNames> queryNamesWrapper = new LambdaQueryWrapper<>();
        queryNamesWrapper.eq(DroolsRuleNames::getDroolsRuleId, droolsRules.getId());
        queryNamesWrapper.eq(DroolsRuleNames::getRuleName, ruleName);
        droolsRulesNamesMapper.delete(queryNamesWrapper);

        super.deleteDroolsRule(kieBaseName, packageName, ruleName);

        queryNamesWrapper = new LambdaQueryWrapper<>();
        queryNamesWrapper.eq(DroolsRuleNames::getDroolsRuleId, droolsRules.getId());
        List<DroolsRuleNames> droolsRuleNames = droolsRulesNamesMapper.selectList(queryNamesWrapper);
        if (CollectionUtils.isEmpty(droolsRuleNames)) {
            droolsRulesMapper.deleteById(droolsRules.getId());
            String file = "src/main/resources/" + packageName + "/" + droolsRules.getId() + ".drl";
            kieFileSystem.delete(file);
        } else {
            Matcher m = p.matcher(droolsRules.getRuleContent());
            while (m.find()) {
                if (m.group(1).equals(ruleName)) {
                    droolsRules.setRuleContent(droolsRules.getRuleContent().replace(m.group(0), ""));
                }
            }
            droolsRulesMapper.updateById(droolsRules);
            writeRules(droolsRules);
        }
        buildKieContainer();
    }
}
