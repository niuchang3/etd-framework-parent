# etd-framework-starter-mybatis

基于 MyBatis-Plus 的持久层 starter，适用于本仓库 Java 21 / Spring Boot 3.5.x 应用。提供分页、防全表更新删除、多租户隔离、组织与本人数据权限、公开读取、自定义查询逻辑删除过滤、实体基础字段、SpEL 自动填充以及雪花 ID 配置。

> 当前 Java 包名为 `org.etd.framework.starter.mybaits`（注意 `mybaits` 拼写），示例导入必须与源码一致。

## 目录

- [能力总览与执行顺序](#能力总览与执行顺序)
- [接入方式](#接入方式)
- [配置项与实际默认行为](#配置项与实际默认行为)
- [实体基类与自动填充](#实体基类与自动填充)
- [分页与条件构造](#分页与条件构造)
- [多租户隔离](#多租户隔离)
- [数据权限基础规则](#数据权限基础规则)
- [数据权限与公开读取](#数据权限与公开读取)
- [自定义查询的逻辑删除与租户过滤](#自定义查询的逻辑删除与租户过滤)
- [扩展点与能力边界](#扩展点与能力边界)
- [验证方式](#验证方式)

## 能力总览与执行顺序

| 能力 | 实现入口 | 生效条件与边界 |
| --- | --- | --- |
| MyBatis-Plus 集成 | `MybatisConfig` | 提供 `MybatisPlusInterceptor`，业务自行声明实体和 Mapper |
| 多租户行级隔离 | `TenantLineInnerInterceptor` / `EtdTenantLineHandler` | 显式开启；从 `RequestContext` 读取租户 |
| 组织、本人数据权限 | `EtdDataPermissionInterceptor` / `EtdDataPermissionHandler` | 显式开启；依据当前用户的权限类型和组织范围生成 SQL |
| 公开读取 | `@DataPermission` / `@DataPermissionTable` | 开启数据权限后按表声明布尔公开字段；只扩展 SELECT |
| 自定义 SELECT 逻辑删除 | `EtdLogicDeleteInterceptor` | 显式开启；依赖已注册实体的 `@TableLogic` 元数据 |
| 分页 | `PaginationInnerInterceptor` | 始终注册；使用 MyBatis-Plus 分页参数 |
| 防全表更新删除 | `BlockAttackInnerInterceptor` | 始终注册；拦截危险的全表 UPDATE / DELETE |
| 基础实体 | `BaseEntity` / `OrgScopedEntity` | 提供主键、审计、租户、逻辑删除及可选组织归属字段 |
| 声明式自动填充 | `DataFillHandler` / `@TableFieldExtend` | 按字段填充阶段计算 SpEL，仅填充 null 字段 |
| 雪花 ID | `DefaultIdentifierGenerator` | 配置 worker / datacenter，供 `ASSIGN_ID` 使用 |
| 空值条件省略 | `EtdLambdaQueryWrapper` | 对已重写的条件方法跳过空参数 |

拦截器按以下顺序注册：**租户（可选）→ 数据权限（可选）→ 逻辑删除（可选）→ 分页 → 防全表更新删除**。三个可选过滤能力互相独立，分页在过滤后执行。

防全表更新删除不等同于业务授权；租户或数据权限追加条件后，语句仍可能影响范围内的多条记录，业务必须明确主键和操作条件。

## 接入方式

在使用本仓库依赖管理的应用中添加依赖，版本由父 POM / BOM 统一管理：

```xml
<dependency>
    <groupId>org.etd.framework</groupId>
    <artifactId>etd-framework-starter-mybatis</artifactId>
</dependency>
```

自动配置通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册 `MybatisConfig` 和 `DataFillAutoConfiguration`，应用无需重复导入这些配置类。

应用还需要：

1. 配置实际使用的数据源、数据库驱动和连接凭证；凭证通过环境变量注入。
2. 使用 `@Mapper` 或应用自身的 `@MapperScan` 注册业务 Mapper，并配置实际 XML 路径。
3. 为数据库表建立与实体一致的列，显式开启实际需要的过滤能力。
4. 由认证或任务入口建立可信的 `RequestContext`，提供租户及用户信息，并在执行结束后清理上下文。

本模块当前依赖包含 MyBatis-Plus Boot 3 starter、JSQLParser、Spring AOP、Druid、MySQL 驱动、ShardingSphere-JDBC 和 common-core。引入依赖不代表已经配置数据源、分片规则或组织权限数据。

业务应用共享组件配置统一维护在 `etd-business-parent/etd-business-common/src/main/resources/`，应用通过 `spring.config.import` 按需导入 classpath 根目录文件，例如 `classpath:druid.yml`。使用 ShardingSphere 的应用由对应环境的 `sharding-<业务域>-<profile>.yaml` 管理物理连接池，不再导入普通 Druid 数据源配置。本 starter 不提供默认分片规则。

## 配置项与实际默认行为

除下表特别说明外，配置前缀均为 `etd.mybatis`：

| 配置项 | 属性初始值 | 当前实现的实际行为 |
| --- | --- | --- |
| `tenant.enabled` | `false` | 缺省不启用租户 SQL 拦截；`true` 时开启 |
| `tenant.column-name` | `tenant_id` | 租户列名 |
| `tenant.ignore-tables` | `[]` | 不追加租户条件的表名列表，匹配时忽略大小写 |
| `data-permission.enabled` | `false` | 缺省不启用数据权限拦截及对应切面 |
| `data-permission.default-org-column` | `org_id` | 默认组织列名 |
| `data-permission.default-user-column` | `create_by` | 默认本人归属列名 |
| `data-permission.ignore-tables` | `[]` | 不追加组织 / 本人条件的表名列表，独立于租户忽略表 |
| `logic-delete.enabled` | `false` | 缺省不为自定义 SELECT 追加逻辑删除条件 |
| `fill.enabled` | `false` | **配置缺省仍注册默认填充器**；显式 `false` 才阻止默认填充器注册 |
| `idGenerator.enabled` | `false` | **配置缺省仍注册本模块 ID 生成器**；显式 `false` 才阻止注册 |
| `idGenerator.worker-id` | `1` | 雪花算法机器 ID |
| `idGenerator.datacenter-id` | `1` | 雪花算法数据中心 ID |
| `snapshot.enabled` | `false` | 仅有属性声明，当前模块未实现快照功能 |

`fill.enabled`、`idGenerator.enabled` 的 Bean 条件使用 `matchIfMissing = true`，所以不能根据属性对象中的 `false` 推断缺省时关闭。ID 生成器的条件前缀在当前源码中写作 `etd.mybatis.idGenerator`，下面示例保留该拼写，避免与条件判断不一致。建议显式配置这些开关。

`IgnoreTenantAspect` 在 `tenant.enabled` 缺省或为 `true` 时注册，但租户 SQL 拦截器仍只有在属性为 `true` 时加入链路。

以下示例展示显式开启各项能力，需按应用表结构和权限需求选择：

```yaml
etd:
  mybatis:
    tenant:
      enabled: true # 启用租户隔离，默认不启用。
      column-name: tenant_id # 所有参与租户隔离的表使用该租户列。
      ignore-tables: [] # 仅列出确实无需租户隔离的公共表。
    data-permission:
      enabled: true # 启用组织与本人数据范围过滤，默认不启用。
      default-org-column: org_id # 默认组织归属字段。
      default-user-column: create_by # 默认本人归属字段。
      ignore-tables: [] # 公共系统表应按实际权限设计显式配置。
    logic-delete:
      enabled: true # 为已注册且声明@TableLogic的表过滤自定义SELECT。
    fill:
      enabled: true # 注册默认SpEL填充器；显式false阻止默认实现注册。
    idGenerator:
      enabled: true # 注册本模块雪花ID生成器；条件前缀沿用源码拼写。
      worker-id: ${MYBATIS_WORKER_ID:1} # 机器ID，多实例部署需规划唯一组合。
      datacenter-id: ${MYBATIS_DATACENTER_ID:1} # 数据中心ID，与机器ID一起区分实例。
```

## 实体基类与自动填充

### 基础字段

`BaseEntity` 提供下列字段，业务表应有对应的数据库列：

| Java 字段 | 数据库列 | 类型 / 注解 | 填充行为 |
| --- | --- | --- | --- |
| `id` | `id` | `Long` / `@TableId(ASSIGN_ID)` | 交给 MyBatis-Plus 主键生成机制 |
| `createTime` | `create_time` | `Instant` | INSERT 时取 `Instant.now()` |
| `createBy` | `create_by` | `Long` | INSERT 时取当前用户 ID |
| `updateTime` | `update_time` | `Instant` | INSERT / UPDATE 时取 `Instant.now()` |
| `updateBy` | `update_by` | `Long` | INSERT / UPDATE 时取当前用户 ID |
| `dataStatus` | `data_status` | `Integer` | INSERT 时取 `BasicConstant.DataStatus.ENABLED.getCode()` |
| `tenantId` | `tenant_id` | `Long` | INSERT 时取 `RequestContext.getTenantCode()` |
| `delFlag` | `del_flag` | `Integer` / `@TableLogic` | 未删除值 `0`，删除值 `1`；未声明 SpEL 自动填充 |

建表时应为 `del_flag` 设置未删除默认值。`data_status` 是业务状态，与逻辑删除字段含义不同。PostgreSQL 中的 `create_time`、`update_time` 使用 `timestamp with time zone`。

具有独立组织归属且需要组织权限过滤的实体继承 `OrgScopedEntity`；它在 `BaseEntity` 上增加 `orgId` / `org_id`，INSERT 且字段为 null 时从当前用户的主组织 `orgId` 填充，UPDATE 不自动变更。公共系统实体继续继承 `BaseEntity`。显式指定组织的授权校验、缺少主组织时的必填校验由业务层负责。

### 声明字段默认值

```java
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.etd.framework.starter.mybaits.core.OrgScopedEntity;
import org.etd.framework.starter.mybaits.fill.annotation.TableFieldExtend;

/** 示例：具有组织归属的业务记录。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_record")
public class RecordEntity extends OrgScopedEntity {

    /** 是否锁定，新增且未指定时默认为未锁定。 */
    @TableField(value = "locked", fill = FieldFill.INSERT)
    @TableFieldExtend("false")
    private Boolean locked;
}
```

默认填充器要求字段同时声明 `@TableField(fill = ...)` 与 `@TableFieldExtend`。`INSERT`、`UPDATE`、`INSERT_UPDATE` 分别决定执行阶段；`value` 非空时优先使用，否则读取 `expression`。

表达式支持常量、`T(完整类名).静态方法()`、`new 完整类名()` 和 `@beanName.method()`，结果转换为字段类型。表达式必须来自可信的后端代码。

填充只在字段当前值为 null 且表达式结果非 null 时赋值，**更新时也不会覆盖已有非 null 的审计字段**。若直接更新刚查询出的实体，需要留意已有 `updateTime` / `updateBy` 不会被自动刷新。表达式解析或求值失败时记录警告并跳过该字段，不能把自动填充作为必填校验。

该机制依赖 MyBatis-Plus 的实体填充回调；纯 XML SQL 或不携带实体的更新不能假定会触发填充。新增默认状态使用声明式填充，启用、禁用、锁定等业务状态更新仍由对应业务方法表达。

## 分页与条件构造

分页拦截器始终注册，业务可使用 MyBatis-Plus 的 `Page` 调用 `BaseMapper.selectPage`：

```java
// RecordEntity 为上文实体，recordMapper 为其已注册的 BaseMapper。
var query = new EtdLambdaQueryWrapper<RecordEntity>();
query.eq(RecordEntity::getLocked, locked);
var page = recordMapper.selectPage(new Page<RecordEntity>(pageNumber, pageSize), query);
```

上述类型分别导入 `org.etd.framework.starter.mybaits.core.EtdLambdaQueryWrapper` 和 `com.baomidou.mybatisplus.extension.plugins.pagination.Page`。自定义分页 SQL 仍需在 Mapper 中声明分页参数与查询。当前配置未指定固定数据库类型、单页上限或越界回首页策略；页码、页大小及排序字段的校验由业务入口负责。

`EtdLambdaQueryWrapper<T>` 对以下方法及对应的 `condition` 重载增加空值检查：

- 单值比较：`eq`、`ne`、`lt`、`le`、`gt`、`ge`。
- 模糊查询：`like`、`likeLeft`、`likeRight`。
- 区间：`between`、`notBetween`，两端均非空才追加，不自动转换为单边范围。
- 集合：`in`、`notIn`，支持集合和可变参数，空集合 / 空数组直接省略条件。

判空使用 Spring `ObjectUtils.isEmpty`；null、空字符串、空集合等会被省略，纯空白字符串不会被当作空值，`false` 和 `0` 仍参与查询。其他继承方法没有统一增加该检查。空 `in` 会扩大查询范围，业务上“空 ID 集合应返回空结果”的场景需要在调用前明确处理。

## 多租户隔离

开启 `etd.mybatis.tenant.enabled=true` 后，租户处理器从 `RequestContext.getTenantCode()` 读取数值租户 ID，并使用配置的租户列参与 MyBatis-Plus 租户 SQL 改写。INSERT 的租户列补充由 MyBatis-Plus 租户拦截器处理，已有租户列不会因此获得业务授权校验。

缺少租户时返回 SQL `NULL` 表达式，不会自动回退到默认租户。应用必须在需要隔离的入口建立有效上下文，不能把缺少上下文视为跨租户访问方式。

以下情况跳过租户过滤：

- `RequestContext.getIgnoreTenant()` 为 true。
- 当前用户 `platformAdmin = true`。
- 表名命中 `tenant.ignore-tables`。
- Mapper 使用 MyBatis-Plus 的 `@InterceptorIgnore(tenantLine = "true")`。

`@IgnoreTenant` 只能标注方法，在经过 Spring AOP 代理调用时临时设置忽略标记，退出时通过 `finally` 恢复原状态，支持嵌套调用。它只跳过租户隔离，组织权限和逻辑删除仍独立执行；同类内部调用不会经过代理。

平台管理员跨租户、跨组织访问由框架放行。租户管理员只跳过组织数据权限，不会因此跳过租户隔离。业务 XML 不应为限制平台管理员额外手写当前租户条件或表间租户等值条件。

## 数据权限基础规则

开启 `etd.mybatis.data-permission.enabled=true` 后，默认对解析到的参与过滤表追加权限条件，**无需每个方法都添加 `@DataPermission`**。没有组织或本人字段的公共表需要显式配置忽略，或在职责明确的调用范围声明目标表。

权限来源是 `RequestContext.getUser()` 中的 `UserDetails`，不是请求参数：

| 权限类型（`BasicConstant.PermissionType`） | code | SQL 行为 |
| --- | --- | --- |
| `ALL` | `1` | 不追加组织 / 本人范围条件 |
| `SELF` | `2` | 当前表本人列等于当前用户 ID |
| `ORGANIZATION` | `3` | 当前表组织列属于 `scopeOrgIds` |
| `ORGANIZATION_AND_SUBORDINATE` | `4` | 当前表组织列属于已展开的 `scopeOrgIds` |
| `CUSTOM_ORGANIZATION` | `5` | 当前表组织列属于自定义 `scopeOrgIds` |

多种角色范围使用 OR 合并。只有存在对应权限类型时才使用用户 ID 或组织集合，框架不因为用户有主组织就自动授予组织范围。组织集合只有一个 ID 时生成等值条件，多个 ID 时生成 `in`。

对实际参与过滤的表，无登录用户、无有效权限类型或无可用范围时生成 `1 = 0`；显式公开读取的例外见下文。平台管理员和租户管理员（`UserDetails.isAdmin()`）均跳过组织 / 本人过滤，`ALL` 也不限制组织范围；是否继续限制租户由租户拦截器独立决定。

`@DataPermission` 可标注 Spring 代理对象的类或方法，方法声明优先于类声明；嵌套作用域使用栈保存并在退出时恢复。主要属性如下：

| 属性 | 默认值 | 用途 |
| --- | --- | --- |
| `enable` | `true` | 当前声明范围是否追加数据权限；`false` 跳过 |
| `tables` | 空数组 | 非空时只过滤匹配的真实表引用；空时按全表 / 外层别名范围过滤 |
| `alias` | 空字符串 | 限定外层目标表引用，例如 `r` 或 `r.` |
| `orgColumn` | 空字符串 | 覆盖全局组织列名 |
| `userColumn` | 空字符串 | 覆盖全局本人列名 |

忽略方式包括类 / 方法上的 `@IgnoreDataPermission`、`RequestContext` 忽略标记、`data-permission.ignore-tables` 和 Mapper 的 `@InterceptorIgnore(dataPermission = "true")`。忽略注解退出时恢复进入前状态。它们不会同时跳过租户与逻辑删除。

注解规则通过 Spring AOP 建立作用域，建议放在经过代理的 Service / Biz 方法上；不要假定任意 Mapper 注解或同类内部调用都会触发该切面。`DataPermissionContextHolder` 使用普通 `ThreadLocal` 保存栈，不自动跨线程传播，异步或后台任务必须在执行线程建立所需上下文并清理。

## 数据权限与公开读取

开启 `etd.mybatis.data-permission.enabled=true` 后，框架通过 `EtdDataPermissionInterceptor` 自动为SQL追加数据范围条件。普通查询无需在XML中拼本人或组织权限。

### 声明公开读取

在经过Spring代理调用的业务查询方法上声明真实表名和布尔公开字段。别名由SQL解析器自动取得，不必重复填写：

```java
/** 查询当前租户内公开或当前角色范围内的文档。 */
@DataPermission(tables = {
    @DataPermissionTable(table = "biz_document", publicColumn = "is_public")
})
public List<DocumentEntity> fetchDocumentList() {
    return documentMapper.selectDocumentList();
}
```

对 `FROM biz_document d` 生成的权限条件为：

```sql
原查询条件 AND (d.is_public = true OR d.create_by = 当前用户 OR d.org_id IN (角色组织集合))
```

只有用户具备的角色范围才会生成对应分支。已登录但没有角色范围时只允许公开数据；没有登录上下文时仍拒绝。拥有全部数据范围或管理员权限时沿用原有放行逻辑。公开不代表匿名开放，也不替代接口权限校验。

### 多表与没有公开字段的表

```java
@DataPermission(tables = {
    @DataPermissionTable(table = "biz_document", publicColumn = "is_public"),
    @DataPermissionTable(table = "biz_category", publicColumn = "is_public"),
    @DataPermissionTable(table = "biz_record")
})
```

- `tables` 非空时是当前作用域的过滤目标清单，未匹配表不追加数据权限；必须列出所有真正需要过滤的表。该清单同样影响此作用域内的写语句，建议只标注职责单一的查询方法，避免在同一范围编排无关表的写入。
- 没有公开字段的表不填写 `publicColumn`，继续按本人、组织等普通数据范围过滤；框架不探测数据库字段，也不要求所有表都有 `is_public`。
- `tables` 为空时沿用原有全表过滤及外层 `alias`、`orgColumn`、`userColumn` 设置，不启用公开扩展。外层 `alias` 仍是整个声明的限制，自动识别别名时将其留空。
- 默认匹配真实表名的每次引用；自关联需要只过滤某个引用时，可填写表级 `alias`。可以使用 `schema.table` 精确限定schema；无schema声明匹配所有同名表。
- 表级 `orgColumn`、`userColumn` 优先于外层注解及框架默认字段。表名、别名及列名必须为后端固定的普通标识符，禁止传入SQL表达式。
- 同一个表引用匹配多条规则会直接报错，不按声明顺序择一放行。公开字段必须在数据库中存在且类型为布尔；配置错误由执行时报错暴露，不降级授权。

### SQL与操作边界

SELECT 使用 `公开 OR 角色范围`；UPDATE 和 DELETE 始终使用严格角色范围，公开不扩大写权限。公开读取不会跳过租户过滤；平台管理员仍由租户处理器按最高权限放行。

沿用MyBatis-Plus的多表SQL遍历及JOIN语义：主表条件进入WHERE，LEFT JOIN右表条件通常进入ON，因此表示“仅关联可读右表记录”，不等价于要求主表同时具备右表权限。INNER JOIN通常要求两侧可读。业务必须选择符合场景的JOIN方式，不能把“每个关联表都过滤”当成跨表授权规则。

已覆盖自动别名、普通JOIN、LEFT JOIN、同表自关联、EXISTS子查询、派生表、UNION和递归CTE的解析测试。CTE名称应与真实业务表名区分，框架按解析器提供的表引用匹配，不推导视图或CTE字段血缘。自定义数据库语法仍需添加对应SQL测试。

目录祖先补齐、“目录下存在公开文件”等跨表业务可见性规则继续由业务SQL表达；公开规则不会自动把不可读祖先变为可读。`@InterceptorIgnore(dataPermission = "true")`、忽略表或忽略数据权限上下文仍会跳过过滤；启用框架规则的查询应去掉对应的跳过声明和重复手写权限条件。

注解依赖Spring AOP上下文传播；同类内部调用不会经过代理。此扩展没有改变现有注解上下文的传播方式。

## 自定义查询的逻辑删除与租户过滤

应用可明确开启自定义SELECT逻辑删除过滤：

```yaml
etd:
  mybatis:
    logic-delete:
      enabled: true # 根据已注册实体的@TableLogic过滤自定义SELECT；默认false。
```

`EtdLogicDeleteInterceptor` 使用已注册Mapper的实体元数据，不猜测哪些表存在 `del_flag`。只有声明了 `@TableLogic` 的表才追加其未删除条件，自动使用SQL别名；没有声明的表和CTE引用不会被盲目追加字段。关联的实体Mapper也必须在查询前注册；显式schema名称需要与实体表名声明一致。

内置BaseMapper生成的未删除条件若已作为必需AND条件存在，则不重复追加；仅在OR分支出现同名条件不视为完成过滤。逻辑删除独立于租户、组织权限与管理员放行：忽略组织权限或租户隔离不会同时显示已删除记录。确有读取已删除记录的维护需求，可在Mapper方法使用 `@InterceptorIgnore(others = {"logicDelete@true"})` 单独跳过。

本能力只扩展SELECT，不转换自定义物理DELETE，也不改写自定义UPDATE；内置写操作继续由MyBatis-Plus的 `@TableLogic` 处理。物理清理或特殊维护语句仍需明确业务边界。

已覆盖普通查询、JOIN、子查询、UNION及递归CTE。SQL解析器的遍历形态存在限制：算术表达式中包裹的标量子查询不能假定会被遍历。此类统计可结合实际查询改写为可遍历的 `UNION ALL` CTE 计数；新增特殊SQL形态必须补充真实结果验证。

开启租户拦截时，普通XML查询不必重复传入当前租户参数。平台管理员具有最高系统权限，框架跳过租户及组织数据权限，不为防止其跨租户访问额外保留表间租户等值条件。采用全局雪花ID的业务记录及跨租户后台任务按唯一ID定位，不重复传递租户过滤参数；普通用户的隔离由框架负责。

## 扩展点与能力边界

- **自定义填充器**：声明继承 `DataFillHandler` 的 Spring Bean，默认实现通过 `@ConditionalOnMissingBean(DataFillHandler.class)` 退让。显式关闭 `fill.enabled` 只阻止默认实现注册，不会关闭应用自己的填充器。
- **ID 生成器**：当前 `identifierGenerator` Bean 没有 `@ConditionalOnMissingBean` 退让条件。应用提供自己的生成器时应显式关闭 `etd.mybatis.idGenerator.enabled`，并核对 MyBatis-Plus 最终使用的生成器。关闭本模块生成器不等于关闭 MyBatis-Plus 的主键生成机制。多实例必须规划不冲突的 worker / datacenter 组合。
- **追加拦截器**：`MyBatisUtils.addInterceptor(interceptor, inner, index)` 可向已有链路的指定索引插入内部拦截器。该工具只调整列表，不检查顺序语义；应在初始化阶段使用并核对与权限、分页的关系。默认 `MybatisPlusInterceptor` Bean 也没有缺失时退让条件，不能假定声明另一个同类型 Bean 就会自动替换。
- **组织子树接口**：`OrgSubtreeResolver.selectSubtreeIds(Long)` 定义组织及下级节点 ID 的解析契约。当前 `EtdDataPermissionHandler` 直接消费用户的 `scopeOrgIds`，不会调用该接口自动展开；组织树范围必须由权限装载流程准备好。
- **快照**：`snapshot.enabled` 尚无执行链路，开启它不会产生数据快照或审计历史。
- **其他插件**：本模块未注册乐观锁插件、动态表名插件或数据变更快照插件，不应将这些能力视为引入 starter 后自动生效。
- **SQL 支持范围**：权限与逻辑删除依赖 JSQLParser / MyBatis-Plus 的 SQL 遍历能力，不代表支持任意数据库语法；特殊 JOIN、CTE、子查询及数据库专有语法应结合实际数据库验证结果和执行计划。组织、本人、租户等过滤字段与关联、排序字段需要按实际查询设计索引，框架不会创建索引。

## 验证方式

在仓库根目录使用 Maven Wrapper 编译该模块及其依赖：

```bash
./mvnw -pl etd-framework-starter/etd-framework-starter-mybatis -am -DskipTests compile
```

执行现有权限和逻辑删除聚焦测试（依赖模块没有匹配测试时不报错）：

```bash
./mvnw -pl etd-framework-starter/etd-framework-starter-mybatis -am \
  -Dtest=EtdDataPermissionHandlerTest,PublicReadDataPermissionTest,EtdLogicDeleteInterceptorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

现有测试覆盖权限配置绑定、管理员放行、本人 / 组织范围、公开读取与严格写权限、Spring 切面上下文、JOIN / 子查询 / UNION / 递归 CTE，以及逻辑删除条件追加、去重和单独忽略。这些主要是 SQL 改写与组件测试，不能替代目标数据库上的启动、分页结果、索引执行计划和特殊 SQL 验证。

## 乐观锁与统一角色范围

框架注册MyBatis-Plus原生 `OptimisticLockerInnerInterceptor`。实体只有显式声明 `@Version` 才参与乐观锁，不在公共BaseEntity中强制新增数据库列。AI六个已有版本字段已启用该注解，初始化继续由 `@TableFieldExtend("0")` 声明式填充。

更新实体携带前端读取时的版本，调用 `updateById(entity)` 或 `update(entity, wrapper)`，由插件比较数据库版本并递增；业务只检查影响行数。不传版本的版本化实体更新直接拒绝，不能静默绕过并发控制；HTTP更新请求通过 `UpdateRequest` 分组提前校验必填版本，新增仍可省略。不要复用同一个Wrapper或在冲突后继续复用已被插件递增版本的实体。

`update(null, wrapper)` 没有实体版本，不能当作带版本编辑入口；上传初始化的状态CAS等明确业务状态转换可继续使用。`processing_version`、`replace_version` 是处理轮次与上传快照，不等价于乐观锁字段，不能机械删除。

`DataPermissionScope` 是角色范围的统一解析入口，SQL处理器、页面writable标记及新增组织资料策略复用相同规则；业务查询不再重复拼租户、本人或组织条件，也不在查出记录后再次计算同一套数据权限。上传任务归属及知识库关系等仍属于业务校验。

共享Druid配置允许 `AND 1=0` 的恒假条件，用于框架安全拒绝无数据范围的请求；保留WallFilter及其他SQL防护规则。
