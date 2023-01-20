### <center>Rules规则引擎使用手册</center>

#### 开启MinIo存储服务

```yaml
rules:
  #  drl文件存储目录
  path: rules/*.*
  dbRules:
    #   是否开启数据库模式，默认为false
    enabled: true
```

#### 规则引擎动态CRUD

```java
import DroolsRule;
import com.sdtsoft.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/rules")
public class Demo {

    /**
     * 执行规则
     * url:http://localhost:9999/rules?name=order_rule&price=1000
     * @param name
     * @param price
     * @return
     */
    @GetMapping()
    public Order testRules(String name, Integer price) {
        Order order = new Order();
        order.setPrice(price);
        rulesManage.fireRule(name, order);
        return order;
    }

    /**
     * 增加或修改规则
     * 示例数据
     * {
     "id":"1600377502751125506",
     "kieBaseName": "order_rule",
     "kiePackageName": "com.niuchang.test",
     "ruleContent": "package com.niuchang.test;\nimport com.sdtsoft.entity.Order\n\n\ndialect  \"mvel\"\n\nrule \"level1\"\n\n    salience 1\n    enabled true\n    when\n        $order:Order(price >= 500 && price <=999)\n    then\n        double discountedPrice = $order.getPrice() * 0.95;\n        $order.setPrice((int)discountedPrice);\n        update($order);\n        drools.halt();\nend\n\n\nrule \"level2\"\n    salience 2\n    enabled true\n    when\n         $order:Order(price >= 1000 && price <=1499)\n    then\n        double discountedPrice = $order.getPrice() * 0.9;\n        $order.setPrice((int) discountedPrice);\n        update($order);\n        drools.halt();\nend\n\n\nrule \"level3\"\n    salience 3\n    enabled true\n    when\n        $order:Order(price >= 1500 && price <=1999 )\n    then\n        double discountedPrice = $order.getPrice() * 0.8;\n        $order.setPrice((int)discountedPrice);\n        update($order);\n        drools.halt();\nend\n\n\nrule \"level4\"\n    salience 4\n    enabled true\n    when\n        $order:Order(price >= 2000)\n    then\n        double discountedPrice = $order.getPrice() * 0.7;\n        $order.setPrice((int)discountedPrice);\n        update($order);\n        drools.halt();\nend",
     "description": "测试订单"
     }
     * @param droolsRule
     */
    @PostMapping
    public void addOrUpdate(@RequestBody DroolsRule droolsRule) {
        rulesManage.addRules(droolsRule);
    }

    /**
     * 删除规则
     * @param kieBaseName
     * @param packageName
     * @param ruleName
     */
    @DeleteMapping
    public void delete(String kieBaseName, String packageName, String ruleName) {
        rulesManage.deleteDroolsRule(kieBaseName, packageName, ruleName);
    }
}
```

#### 规则文件的语法说明

```drools

package // 包名，package对应的不⼀定是真正的⽬录，可以任意写com.abc，同⼀个包下的drl⽂件可以相互访问
// 引入Java中的类，需要些全限定名
import  // Optional ⽤于导⼊类或者静态⽅法

global   // Optional 全局变量

// 方言可选java
dialect  "mvel"
// rule 关键字 "rule name" 规则的名字，该名字全局唯一
rule "rule name"
    //  Optional
    when
    // Conditions  条件，可为空
    then
    // Actions // 匹配后执行的结果
    //  终止其他规则执行      
        drools.halt(); 
end // 关键字
```
#### drl属性 可选项
|属性|描述|
| :---: | :---: |
|  salience   |    定义规则优先级的整数，数值越大，优先级越高   |
|  enabled   |    规则启用开关   |
|  date-effective   |    包含日期和时间定义的字符串。仅当当前日期和时间在date-effective属性之后时，才能激活该规则。   |
|  date-expires   |    如果当前日期和时间在date-expires属性之后，则无法激活该规则。   |
|  no-loop   |    选择该选项后，如果规则的结果重新触发了先前满足的条件，则无法重新激活（循环）规则。如果未选择条件，则在这些情况下可以循环规则。   |
|  dialect   |    用于标识规则中的代码表达式JAVA或MVEL将其用作语言   |