# AI 编码规范

本文件是当前仓库的全局 AI 编码提示词。任何 AI Agent 在修改本仓库代码前都必须先阅读本文件，并严格遵守。除非用户给出了更新、更具体的指令，否则以本文件为准。

## 项目基线

- Java 版本：21。
- Spring Boot 版本：3.5.x。
- Spring Cloud 版本：2025.x。
- 当前项目是多模块 Maven 框架工程，修改时必须保持现有模块结构清晰、职责稳定：
  - `etd-framework-commons`
  - `etd-framework-starter`
  - `etd-framework-dependencies`
  - `etd-business-parent`
  - `demo`
- 优先使用仓库根目录下的 Maven Wrapper：
  - macOS/Linux：`./mvnw`
  - Windows：`mvnw.cmd`

## 不可违反的规则

- 不要在已有 Java 21 / Spring Boot 3 可用方案的情况下，引入 Java 8 时代的 API、配置方式或旧写法。
- Jakarta EE 相关 API 不要使用 `javax.*`，必须使用 `jakarta.*`。JDK 自带的 `javax.sql.*` 可以使用。
- 不要硬编码密钥、密码、私钥、访问令牌、数据库地址、生产环境地址或任何敏感信息。
- 不要提交 IDE 配置、构建产物、日志、本地运行数据等生成文件。
- 修复具体问题时，不要顺手做无关重构。
- 未经用户明确要求，不要修改公共 API、包名、模块名、starter 行为或接口协议。
- 不要用大范围注解粗暴压制 warning。只有理解 warning 原因后，才允许做小范围、明确的抑制。
- 不要因为方便就新增依赖。优先使用 JDK、Spring Boot 和项目里已经存在的依赖。

## 依赖管理

- 优先使用 Spring Boot BOM 和 Spring Cloud BOM 管理依赖版本。
- Spring Boot 已经管理的依赖版本，不要手动覆盖，除非有明确的兼容性或安全原因。
- 如果确实需要手动指定某个依赖版本，必须在属性或依赖附近说明原因。
- `etd-framework-dependencies` 是共享依赖版本管理的中心，公共版本不要散落到业务模块里。
- 修改依赖版本前，至少要检查受影响应用模块的 dependency tree。
- 避免同一依赖家族出现混合版本，尤其是：
  - Jackson
  - Spring
  - Spring Security
  - Netty
  - MyBatis Plus
  - Elasticsearch 客户端相关依赖

## Spring Boot 3 规范

- starter 自动配置类必须通过以下文件注册：
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 不要只依赖 `spring.factories` 注册自动配置。
- starter 自动配置类在合适时优先使用 `@AutoConfiguration`。
- starter 中避免继承 `WebMvcConfigurationSupport`，优先实现 `WebMvcConfigurer`，避免破坏 Spring Boot MVC 自动配置。
- Redis 配置使用 `spring.data.redis.*`，不要再使用旧的 `spring.redis.*`。
- 本地 Redis 无密码时，直接省略 password 配置，不要设置空密码或假密码。
- Spring Security 新代码要使用 Spring Security 6 风格 API，避免继续使用废弃的 `http.apply(...)` 和废弃的 request matcher API。

## 序列化与缓存

- Redis JSON 序列化和 HTTP JSON 处理优先使用 Jackson。
- Jackson 版本统一交给 Spring Boot BOM 管理。
- 新代码不要再调用已废弃的 Redis Jackson 序列化 API，例如 `setObjectMapper(...)`。应使用当前 Spring Data Redis 支持的构造方式。
- 谨慎启用 Jackson 多态类型信息。不要在未理解安全影响的情况下开启过宽的 default typing。

## 时间类型与时区

- 创建时间、修改时间、登录时间、过期时间等表示绝对时间点的字段统一使用 `Instant`，禁止在业务实体、DTO、VO 中使用 `Date` 或 `LocalDateTime` 代替时间点。
- 生日、业务日期等不包含时间和时区语义的字段使用 `LocalDate`；纯时间使用 `LocalTime`。
- `Date` 只允许出现在第三方 API 明确要求该类型的边界适配代码中，例如 Nimbus JWT、OSS SDK，进入业务模型后必须转换为 `Instant` 或 `LocalDate`。
- HTTP 时间点统一按 `spring.jackson.time-zone` 配置的时区输出 `yyyy-MM-dd'T'HH:mm:ss.SSSXXX`；未配置时默认使用 `Asia/Shanghai`。
- HTTP 纯日期统一输出 `yyyy-MM-dd`，纯时间统一输出 `HH:mm:ss`。前端不得自行给接口时间固定增加或减少小时数。
- PostgreSQL 中绝对时间点使用 `timestamp with time zone`，纯日期使用 `date`，不要用无时区 `timestamp` 保存绝对时间点。
- 初始化 SQL 中的固定时间点必须显式携带 `Z` 或偏移量，例如 `2026-09-01 12:30:00.000000+08:00`；动态初始化时间使用 `current_timestamp`。

## 配置规范

- 应用配置必须方便环境化：
  - 凭证使用环境变量；
  - 本地开发只保留安全默认值；
  - 不要把真实 UAT/生产环境凭证提交到仓库。
- 新增配置项时，要判断是否应该放入 `@ConfigurationProperties`。
- 未经用户要求，不要悄悄修改 Redis database、数据源地址、端口、安全路径等行为性配置。

## 常量、枚举与魔法值

- Java 代码中禁止直接散落具有业务含义、协议含义或跨位置约定含义的魔法值。
- 以下内容必须使用语义明确的常量或枚举统一定义：
  - 数据状态、逻辑删除状态、启停状态、审核状态等状态码；
  - 租户类型、角色类型、权限码、菜单类型、业务类型等类型码；
  - 请求头名称、缓存键前缀、Token 字段名、固定 JSON 字段名等协议约定；
  - 在多个位置重复使用的配置键、默认值、边界值和特殊标识值。
- 全局常量和枚举统一放在 `etd-framework-common-core` 的 `org.etd.framework.common.core.constants` 包：
  - 跨模块通用的基础状态优先定义在 `BasicConstant` 中，例如 `BasicConstant.DataStatus`；
  - 具有独立语义、字段较多或需要实现接口的常量应使用独立枚举类，不要持续向 `BasicConstant` 堆积不相关内容；
  - 常量名称使用大写字母和下划线，枚举类型使用大驼峰，枚举项使用大写字母和下划线；
  - 有数据库存储值或接口传输值的枚举必须显式定义 `code`，禁止依赖 `ordinal()`，也不要把枚举项名称作为隐式协议值；
  - 业务判断、查询条件、默认赋值、参数校验和序列化必须引用同一个枚举或常量定义，不能在不同位置重复写字面量；
  - SQL、YAML 等无法直接引用 Java 枚举的文件可以保留必要字面量，但字段注释、默认值和 Java 枚举语义必须一致。
- 只在单个业务模块或单个功能域使用的常量应放在对应功能域的 `constant` 包，例如 `org.etd.order.refund.constant`，不要上提为全局常量。
- 新增常量或枚举前必须先搜索已有定义，禁止为同一语义创建名称不同、取值相同的重复常量。
- 不要创建无边界的 `Constant`、`Constants`、`CommonEnum` 类；常量类或枚举名必须表达具体业务或技术语义。
- 不要求机械提取所有字面量。含义显而易见且只服务于局部技术实现的值可以直接使用，例如集合空判断中的 `0`、数据库影响行数判断中的 `> 0`、数组下标和清晰算法中的局部边界值；一旦具有业务含义、重复出现或需要跨层保持一致，就必须提取为常量或枚举。

## 代码风格

- 遵循项目现有包名、类名和代码组织风格。
- 包命名必须遵循项目现有根命名空间：
  - 框架公共模块使用 `org.etd.framework.common.*`；
  - 框架 starter 模块使用 `org.etd.framework.starter.*`；
  - 业务模块使用 `org.etd.<业务模块名>.<功能域>.<技术分层>`，例如用户管理 Controller 使用 `org.etd.upms.user.controller`，订单退款 Service 使用 `org.etd.order.refund.service`；
  - 业务模块包路径不要包含 `framework`、`business` 这类框架层级，例如不要使用 `org.etd.framework.business.upms.controller`；
  - 已存在的历史包名在未重构前保持兼容，不要为了统一命名擅自搬迁。
- 业务模块必须采用“业务模块优先、功能域其次、技术分层最后”的纵向切片结构：
  - 第一层是业务模块，例如 `upms`、`order`、`product`；
  - 第二层是具体功能域，例如 `user`、`role`、`menu`、`tenant`、`refund`、`payment`；
  - 第三层才是 `controller`、`biz`、`service`、`entity`、`mapper`、`converter` 等技术分层；
  - 不要使用 `org.etd.upms.controller.user`、`org.etd.upms.service.user` 这类技术分层优先的包路径；
  - 功能域名称应直接表达业务概念，优先使用 `user`、`role`，不要使用含义宽泛的 `usermanage`、`rolemanage`。
- 新增包名前必须先检查同模块是否已有相同职责的包，优先放入已有包结构，不要随意创造 `utils`、`helper`、`common`、`config2`、`new`、`test` 等含义模糊的包。
- 包名只使用小写字母和点号，不使用大写、下划线、中划线、拼音缩写或无意义缩写。
- 包层级要表达业务或技术职责，不要过深。业务模块根包之后的业务层级一般不超过 4 层；超过时必须确认确实能提升边界清晰度。
- starter 模块内不要引用业务模块包；common 模块不要反向引用 starter 或 business 包；business 模块可以依赖 starter 和 common 暴露的能力。
- 新增类时，包路径必须和模块职责一致。例如：
  - 通用工具放在 `etd-framework-commons` 下合适的 `common` 包；
  - 自动配置放在对应 starter 的 `config` 或 `autoconfigure` 包；
  - 业务控制器、服务、实体、转换器放在对应功能域下，例如 `org.etd.upms.user.controller`、`org.etd.upms.user.service`、`org.etd.upms.user.entity`、`org.etd.upms.user.converter`。
- 业务模块的 Controller 层按页面或功能入口组织，不要把一个功能域的所有接口都堆进同一个大 Controller。
- Controller 位于对应功能域内部，例如 `user.controller`、`role.controller`、`menu.controller`、`tenant.controller`；页面进一步复杂时，可以在 `controller` 下继续拆分更具体的功能入口。
- Controller 层使用的请求 DTO、响应 VO 应放在对应功能域的 Controller 包下，例如：
  - `user.controller.dto`
  - `user.controller.vo`
  - `role.controller.dto`
  - `role.controller.vo`
- DTO/VO 以接口场景为边界，不要为了复用制造字段过多的全局大 DTO 或大 VO。
- Controller 只负责接口入口、参数校验、DTO/VO 转换和调用 biz/service，不要直接调用 mapper，不要直接写数据库、Redis、MQ 或复杂业务规则。
- 每个功能域可以设置 `biz` 包作为业务编排层，例如 `user.biz`、`role.biz`、`menu.biz`、`tenant.biz`。
- `biz` 层负责完整业务用例编排，例如创建用户并分配角色、角色授权菜单、初始化租户、导入用户、删除菜单并清理关系。
- `biz` 层可以控制事务，可以调用多个 service，但不直接调用 mapper，不写 SQL 细节，不放 Controller DTO/VO。
- 跨多个 Service 的业务组装必须放在 `biz` 包，不要塞进某个大 Service。
- `biz` 层命名使用 `XxxBizService`；复杂到需要独立表达场景时，可以使用 `XxxActionBizService`，例如 `UserImportBizService`、`TenantInitBizService`、`RoleAuthorizeBizService`。
- Service 必须位于对应功能域内部，例如 `user.service`、`role.service`、`menu.service`、`tenant.service`。
- Service 类按业务能力或业务关系拆分，不要把同一业务类型下的所有能力都塞进一个大 Service。
- 允许使用类似 `UserService` 的基础能力 Service，但它只应承载用户基础信息相关能力，例如新增、修改、详情、启用、禁用；关系绑定、密码、安全、日志、导出等独立能力应拆成单独 Service。
- 业务关系应使用明确的 Service 表达，例如 `UserRoleService`、`RoleMenuService`、`TenantPackageService`。不要机械拼接表名，类名必须表达真实业务语义。
- 能力明显独立时应单独成 Service，例如 `UserPasswordService`、`MenuTreeService`、`UserLoginLogService`。
- 不要创建 `SystemService`、`CommonService`、`BaseService`、`ManageService` 这类职责过宽、容易膨胀的业务 Service。
- 单个 Service 不要同时处理基础信息、关系绑定、密码、安全、日志、导入导出、页面展示组装等多个不相关职责。出现明显不相关职责时必须拆分。
- Service 层是能力服务层，负责单一业务能力，可以调用 mapper，但不调用 biz；默认不承担跨多个 Service 的完整流程编排。
- 能力型 Service 默认不相互调用，避免形成循环依赖和隐式调用链。确需组合多个 Service 时，应上移到 `biz` 层。
- 能力型 Service 可以承接 JetCache 缓存职责。稳定查询方法可以使用 `@Cached`，写方法必须同步设计 `@CacheInvalidate` 或主动缓存失效策略。
- `biz` 层不直接添加 JetCache 缓存注解，mapper 层不添加缓存注解。缓存应围绕稳定业务能力，而不是围绕 SQL 细节或编排流程。
- 涉及用户权限、用户角色、角色菜单、菜单树等关联缓存时，必须明确失效范围，不能只加缓存不处理失效。
- Controller 可以调用同一业务类型下的多个 Service 来完成简单页面功能；涉及多个 Service 的写流程或复杂业务组装时，应调用 `biz` 层。
- Mapper 必须位于对应功能域内部，例如 `user.mapper`、`role.mapper`、`menu.mapper`、`tenant.mapper`。
- Mapper 保持简单，负责对应表或关系表的数据访问，例如 `UserMapper`、`UserRoleMapper`、`RoleMapper`、`RoleMenuMapper`。
- Mapper XML 必须和 Java Mapper 一一对应。Java Mapper 如何拆，XML 就如何拆。
- Mapper XML 按功能域放在 `resources/mapper/<功能域>`，并与 Java Mapper 一一对应，例如：
  - `user/mapper/UserMapper.java` 对应 `resources/mapper/user/UserMapper.xml`
  - `user/mapper/UserRoleMapper.java` 对应 `resources/mapper/user/UserRoleMapper.xml`
  - `role/mapper/RoleMapper.java` 对应 `resources/mapper/role/RoleMapper.xml`
- 不允许多个 Mapper 共用一个大 XML，不允许一个 XML 中混写多个不同业务类型或关系类型的 SQL。
- 不要提前为了可能存在的复杂查询创建大量 QueryMapper。复杂查询真实出现，且明显不适合放入基础 Mapper 时，再按查询场景新增对应的 `XxxQueryMapper.java` 和 `XxxQueryMapper.xml`。
- Mapper 不写业务规则，不做业务编排，不直接返回 Controller VO。
- 修改要小而聚焦。
- 优先写清楚直接的代码，不要炫技。
- 避免使用过度复杂、多层嵌套或难以维护的 Stream 流式写法：
  - 严禁在 Stream 算子内部（如 `.filter()`、`.map()`）引入修改外部变量状态或集合的副作用（side-effects）；
  - 严禁为了单纯的集合遍历或带 `try-catch` 的副作用循环而滥用 `.stream().forEach(...)`，纯副作用迭代应直接使用标准的 `for` 循环；
  - 包含 2 个及以上中间算子（如同时包含 `filter` + `map` + `groupingBy`）、涉及算法/复杂分组或业务规则判断的 Stream 表达式，**必须提取为独立且语义明确的私有辅助方法**（如 `groupOrganizationsByUserId`），避免在主业务逻辑中内联长流；仅涉及单一属性映射（如 `list.stream().map(X::getId).toList()`）或简单 `anyMatch` 的极简单行流，允许直接内联。
- 单个方法理论上不超过 30 行。超过 30 行时，必须优先考虑提取私有方法、规则方法、转换方法或独立组件承接复杂逻辑。
- 提取方法时，方法名必须表达业务语义或技术语义，不要用 `handle`、`process`、`doSomething`、`buildData` 这类含糊命名。
- **业务函数命名规范（适用于业务模块及公共模块，`etd-framework-starter` 模块不在本命名规范约束范围内）**：
  - **语序结构**：必须遵循 `[核心动作(动词)] + [操作对象(目标名词)] + [限定条件/维度(如 By/With)]` 的自然语序（如 `createTenantAdmin`、`switchStatus`、`selectUserPage`），严禁使用名词开头的颠倒命名（如 `tenantAdminCreate`）。
  - **单复数显式修饰**：为了符合中文阅读习惯，返回集合/批量查询的方法必须在名称中显式带上集合特征（如 `selectUserList`、`selectUserIds`、`groupOrganizationsByUserId`、`countAccount`），避免仅靠结尾 `s` 区分单复数。
  - **条件查询统一介词**：表达按条件/维度过滤查询时，统一使用 `By` 作为介词（如 `selectByUser`、`selectByUserIds`），减少多种介词混用的阅读转换负担。
  - **技术分层动词规范**：
    - Controller 层：表达前端 HTTP 意图，统一使用 `save` / `remove` / `update` / `get` / `page`；
    - BizService 层：表达完整业务流程，使用 `init` / `grant` / `import` / `assign` / `bind`；
    - Service 层：表达单一能力或规则，使用 `switch` / `require` / `populate` / `resolve`；
    - Mapper 层：表达基础 SQL 动作，统一使用 `select` / `insert` / `update` / `delete`。
  - **禁忌**：严禁使用 `handleData`、`processUser`、`doSomething`、`buildData` 这类无法看出实际逻辑的含糊无意义词汇。
- AI 编写与修改的代码必须包含清晰的中文注释：
  - 新增类与关键方法必须编写中文 JavaDoc 注释；
  - 复杂业务规则、关键分支、算法步骤、非显而易见的处理、兼容性决策、缓存失效原因、事务边界都要有简洁中文注释；
  - 不要为了满足注释要求添加无意义注释，例如“设置用户名”“调用方法”“返回结果”这类代码本身已经清楚表达的内容。
- 不要随意新增宽泛的工具类。只有至少两个真实调用点需要复用时，才考虑抽取。
- 不要在多个 starter 里复制逻辑。确实需要复用时，应放到合适的 common 模块。
- 异常处理要明确，不要静默吞异常。
- 日志要提供有用上下文，但绝不能打印密钥、密码、私钥或访问令牌。

## 测试与验证

- 修改 Java 版本或依赖后，运行：
  `./mvnw -DskipTests compile`
- 修改单个模块后，编译该模块及其依赖：
  `./mvnw -pl <module> -am -DskipTests compile`
- 修改依赖版本后，检查依赖树：
  `./mvnw -pl <module> -DskipTests dependency:tree`
- 修改启动配置后，要验证受影响应用可以启动。
- 修改安全逻辑后，要同时验证公开接口和受保护接口。
- 行为发生变化时，要新增或更新测试。项目缺少测试且改动风险较高时，至少补充聚焦的 smoke test，不要只依赖手工验证。

## Git 与已有改动

- 工作区可能存在用户自己的改动。不要回滚不是你创建的改动。
- 编辑文件前，要先查看附近代码和当前已有 diff。
- 不要把生成文件或无关改动混入本次变更。
- 除非用户明确要求，不要执行 `git reset --hard`、强制 checkout 等破坏性命令。

## AI 回复要求

- 说明改了什么，以及为什么这样改。
- 说明实际执行过的验证命令。
- 如果有验证无法执行，必须明确说明。
- 报告代码问题时，要给出文件路径和行号。
- 回复保持简洁、准确、可执行。
