# 租户模型管理表设计

本设计只覆盖已经确认的 SaaS 租户自带模型连接与模型分类管理，不提供平台代付账户，不包含知识库、文档、分片、向量表或对话会话表。初始化结构见 `src/main/resources/sql/postgresql/schema.sql`，当前不接入启动自动建表。

## 两张表的边界

| 表 | 维护内容 | 示例 |
| --- | --- | --- |
| `ai_model_connection` | 租户的提供商、服务地址、认证方式和加密凭证 | 租户 A 的百炼账户；租户 B 的 Ollama 服务 |
| `ai_model_config` | 连接下的模型类型、模型标识、维度、能力和默认参数 | 同一百炼连接下分别配置聊天模型与向量模型 |

一个连接可以有多个模型；相同真实模型标识也可以保存为不同显示名称和默认参数的调用配置。每条模型配置只有一个主类型，多模态输入、工具调用等通过能力标记表达。例如支持图片理解的聊天模型仍属于 `chat`，不归入图片生成 `image`。

提供商是适配器类别，不是用户填写的 Java 类名。`openai_compatible` 表示实现相应协议的接入方式，不代表任意服务都能支持所有模型类型；类型与提供商的支持组合必须由后端已实现的适配器校验。

## 主要字段

两张表都沿用 `BaseEntity` 的 `id`、创建/修改人及时间、`tenant_id`、`data_status`、`del_flag`，另有 `version` 用于乐观锁和配置缓存版本。时间点使用 `timestamp with time zone`，Java 使用 `Instant`。模型连接属于租户级资源，第一版不引入组织归属或 `org_id`，对应实体应继承 `BaseEntity`。

`data_status` 统一表示启停，取值沿用 `BasicConstant.DataStatus`：`0` 禁用、`1` 启用；不再增加表示相同含义的 `enabled` 列。ID 由框架分配，不使用数据库自增。创建时的默认状态仍应按框架 `@TableFieldExtend` 声明式填充，SQL 默认值不替代后续实体注解要求。

连接的核心字段：

| 字段 | 作用 |
| --- | --- |
| `connection_name` | 租户可读的连接名称 |
| `provider` | `dashscope`、`ollama`、`openai_compatible` |
| `base_url` | 由 SaaS 后端访问的 API 根地址 |
| `auth_type` | `none` 或 `api_key`，页面按连接实际认证要求填写 |
| `api_key_ciphertext` | 可解密的认证加密密文封装，不能使用单向密码哈希 |
| `credential_key_id` | 选择服务端加密主密钥的非敏感标识 |

不在 URL 中携带用户名、密码、API Key 或签名。不同提供商的根地址及协议路径由适配器定义；即使同属一个提供商，不同地域、业务空间或账户也可以建立独立连接。

模型的核心字段：

| 字段 | 作用 |
| --- | --- |
| `connection_id` | 复用本租户的一条连接 |
| `model_name` | 页面显示名称，例如“开发文档向量模型” |
| `model_type` | `chat`、`embedding`、`rerank`、`image`、`speech_to_text`、`text_to_speech` |
| `model_code` | 服务要求的实际模型标识，按原值保存，不自动转小写 |
| `embedding_dimensions` | 向量模型后端实测输出维度，页面只读，其他类型必须为空；基础阶段允许为空，不能手工声明后标记可用 |
| `capabilities` | 类型相关能力 JSON，例如 `{"toolCalling":true,"visionInput":true}` |
| `default_options` | 默认参数 JSON，例如聊天模型的 `{"temperature":0.7,"maxTokens":2048}` |

当前Java实现通过ModelProtocol常量、DTO及DDL限制显式代码，允许保存六种类型的草稿，不创建或调用模型客户端；verified始终为false，不代表相应调用适配器已完成。能力和调用参数JSON本阶段只保留数据库结构，不开放页面任意编辑。

JSON 用于各类型确有差异的能力和调用参数，不承担表关联、启停、凭证或模型身份。后端必须按“提供商 + 模型类型”校验允许的字段及取值，禁止反射绑定任意客户端属性；JSON 中不得覆盖 `base_url`、密钥、`model_code` 或 `embedding_dimensions`。新增能力时再扩展对应参数校验，不预建大量类型扩展表。

## 页面字典

固定选项统一复用 UPMS 的全局内置字典，AI 业务字典初始化脚本由本工程维护，入口为 `src/main/resources/sql/postgresql/data.sql`。部署时先完成 UPMS 字典表建表，再连接 `sys_dict_type`、`sys_dict_data` 所在数据库显式执行本脚本；脚本归属 AI 工程不代表需要在 AI 数据库重复建立字典表。字典只包含公开分类和标签，不包含租户连接、实际模型实例或凭证，应用启动时不自动执行。

| 字典类型编码 | 对应字段 | 字典值 |
| --- | --- | --- |
| `ai_model_type` | `model_type` | `chat`、`embedding`、`rerank`、`image`、`speech_to_text`、`text_to_speech` |
| `ai_model_provider` | `provider` | `dashscope`、`ollama`、`openai_compatible` |
| `ai_model_auth_type` | `auth_type` | `none`、`api_key` |

页面通过 UPMS 已有的 `GET /v1/dict/type/code/{typeCode}/data` 获取选项，以 `dict_label` 展示、`dict_value` 提交。接入协议不等于厂商品牌，兼容相应协议的客户服务选择对应适配器，不需要为每个厂商新增字典项。

字典描述模型类型全集，不表示所有类型与协议的组合都已经实现。当前允许创建草稿，所有配置返回verified=false；未来开放实际调用前再按已实现的适配能力限制可调用组合，不能仅凭字典开放模型执行。字典不能替代后端参数校验、模型表 CHECK 约束和后续 Java 枚举。

以上字典类型及字典项均标记为内置，沿用现有 UPMS 服务对内置字典的修改、停用和删除限制。启停状态复用 `common_status`；模型实例从当前租户已授权的模型列表查询，维度通过测试模型实测，不建立固定模型名称或维度字典。

## 租户隔离与生命周期

- 模型通过 `(tenant_id, connection_id)` 引用连接的 `(tenant_id, id)`，数据库拒绝跨租户关联。对应非部分唯一约束是复合外键的实际需要，不是仅为展示额外创建的索引。
- 前端不决定有效租户，后端从可信上下文取得 `tenant_id`；Mapper、客户端缓存和后台任务都必须携带该租户。两张表不得加入普通租户请求的隔离忽略名单；平台管理员沿用框架已确认的全局管理规则。
- 使用模型时同时检查模型和连接的租户归属、`del_flag=0` 及 `data_status=1`。外键只能保证归属和物理记录存在，不能代替软删除、启停及业务授权校验。
- 新建/迁移模型配置时，需要在同一事务内锁定或以等价方式保护目标连接；逻辑删除连接前也需要保护同一连接记录并检查是否仍有未删除模型，避免并发创建与删除产生失效关联。有关联时拒绝删除，不自动级联删除模型或未来的知识库。
- 停用连接会使其下所有模型不可使用；停用模型只影响这一条配置。修改连接或模型都递增各自 `version`，使用乐观锁拒绝覆盖并发更新；数据库默认值不会自动递增版本或自动刷新修改时间。
- 模型客户端缓存键至少包含 `tenant_id + connection_id + connection.version + model_config_id + model_config.version`。停用、删除、密钥轮换和配置修改需要同步失效所有应用实例的相关缓存；不能只给缓存增加版本后跳过启停和租户校验。
- 运行时客户端只放有界本地缓存，不把客户端实例或解密后的密钥写入 Redis。持有连接凭证的客户端不能对外返回；也不能在共享客户端上修改请求或会话状态。

## 密钥与连接验证

本版已实现只写凭证的AES-GCM保存、脱敏详情和配置CRUD，尚未实现测试连接及模型客户端注册。API合同见 [前端AI实施提示词](frontend-ai-prompt.md)。以下关于实际调用的验证规则属于后续阶段。

密钥采用认证加密，例如 AES-GCM；每次加密生成独立 nonce，并封装密文格式版本、nonce、密文与认证标签。建议认证附加数据包含租户和连接 ID，避免密文被跨记录替换；算法和密钥管理需要在后续服务实现中统一，不在 SQL 中实现加解密。

`credential_key_id` 只保存服务端密钥标识，实际主密钥必须由运行环境提供，不能与密文同库明文存放。列表、详情、日志和异常都不返回原始凭证，可只返回 `hasCredential`；更新时由明确的“保留原凭证/替换凭证/切换认证方式”协议处理，不把掩码占位符当作新密钥。

“测试连接”应按模型类型发起一次明确的实际调用，验证模型是否可用；Embedding 还需核对实际维度。调用可能收费，需由用户显式点击触发，不在保存记录或读取列表时自动调用。第一版不保存容易失真的“永远健康”标志或提前增加独立测试历史表。

租户的 Ollama 地址必须对 SaaS 后端可达。地址提交和实际请求时均需校验协议、主机、解析结果与重定向目的地；公网 SaaS 拒绝访问本机、云元数据及内部管理地址，需要接内网模型时使用受控网络或连接代理，不把任意内网 URL 当作普通租户配置直接请求。

## 查询和索引

| 查询场景 | 索引或约束 |
| --- | --- |
| 当前租户连接列表，创建时间倒序分页 | `idx_ai_model_connection_tenant_created`，仅覆盖未删除记录 |
| 同租户连接名称防重 | `uk_ai_model_connection_tenant_name`，逻辑删除后允许复用名称 |
| 当前租户指定模型分类下分页 | `idx_ai_model_config_tenant_type_created`，索引附带状态用于筛选 |
| 同租户同分类模型名称防重 | `uk_ai_model_config_tenant_type_name` |
| 查询连接下模型、关联查询及外键检查 | `idx_ai_model_config_tenant_connection` |
| 模型关联连接 | 连接的 `(tenant_id,id)` 唯一约束及模型的 `(tenant_id,connection_id)` 索引 |

模型列表应直接 JOIN 同租户连接，不在 Java 中循环补连接名称；建议条件为 `m.tenant_id = :tenantId and m.model_type = :modelType and m.del_flag = 0`，连接条件同时包含 `m.tenant_id = c.tenant_id and m.connection_id = c.id`，连接侧也过滤逻辑删除。按 `m.create_time desc, m.id desc` 分页，与索引顺序一致。

这些索引覆盖查询的关键连接键、筛选键和排序顺序，不宣称任意 `select *` 都能使用索引仅扫描。模糊搜索、JSON 字段筛选或跨分类汇总未在当前范围内，不提前创建无实际查询依据的 GIN 等索引。

## 后续知识库接入边界

知识库需要引用本租户的 Embedding 模型配置，并记录实际使用的模型标识、连接归属及向量维度。不能仅凭配置记录 ID 判断两个向量空间是否一致；模型标识、维度或影响向量结果的参数变更需要按知识库重建索引流程处理，密钥轮换和显示名称修改不应被误判为必须重算向量。

当前 `version` 服务于配置更新和客户端缓存，不直接充当向量空间版本。未来接入知识库时再落实绑定约束和重建流程；本次不提前创建未使用的绑定表或向量表。
