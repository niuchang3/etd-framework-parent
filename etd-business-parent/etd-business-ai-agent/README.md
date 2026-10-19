# AI Agent 业务应用

平台维护全局模型目录，租户选择模型后创建连接器。服务商、模型类型、接入协议与认证方式使用 UPMS 全局字典。目录忽略租户和组织隔离，连接器保留租户隔离。

## 数据与接口

- `ai_model`：开发人员维护的模型目录，无 `tenant_id`；模型代码、Logo、密钥获取链接和接入指南在此维护。`GET /v1/models` 支持 `providerCode`、`modelType`、`current`、`size`；`GET /v1/models/{id}` 返回详情。资源码 `ai:model`，仅提供只读接口，不开放租户维护入口。
- `ai_model_connection`：一条连接器对应一个平台模型，保存 `model_id`、`protocol`、实际服务地址与加密凭证，没有连接名称。`/v1/model-connections` 提供 GET/POST，`/{id}` 提供 GET/PUT/DELETE。资源码 `ai:model-connection`。分页支持 `modelId`、`dataStatus`、`current`、`size`，响应通过 SQL 关联返回模型名称、类型、Logo 和服务商。
- 独立目录：`/v1/knowledge-folders`，资源码 `ai:knowledge-folder`，原部门归属和数据权限语义不变。

模型目录通过服务商字典值分类；一个服务商下有效模型代码唯一，不同服务商可收录相同模型。不从服务商品牌推断协议；租户在连接器中选择协议，当前可保存 `dashscope`、`ollama`、`openai_compatible`。保存配置不表示完成了连通性测试，也不表示所有类型的模型调用均已实现。本次不新增模型调用、模型发现或探测接口。

连接器允许同一租户为同一模型建立多条记录，通过模型、协议、地址及记录ID识别。创建后禁止切换绑定模型，换模型应重新建立连接。API Key 只写，新建 `api_key` 认证必须提供凭证；更新留空保留原密文，切换 `none` 清空密文和密钥标识。更新必须提交当前 `version`。DTO JSON序列化及toString均不输出API Key；响应不输出密文、加密标识或租户身份。

平台目录停用后不再列入可选项，也不允许新增或编辑连接；已建立连接仍可查询或删除。平台目录没有HTTP写接口，部署录入时必须遵守引用约束，不物理删除仍被使用的记录。

## 数据库初始化

`../sql/ai.sql` 包含扩展、检索函数和完整首次建库定义，直接表达当前最终结构，不提供旧表迁移、别名或兼容链路。`ai_model_config` 及对应全部代码已删除，知识库、向量表的标识统一改为模型连接器ID。

SQL 仍包含等待重新设计的知识库、文档及向量表，这些结构不代表已经提供业务接口。为保持引用完整性，知识库通过 `(tenant_id, embedding_model_connection_id, embedding_model_id)` 引用连接器，再通过 `(embedding_model_id, embedding_dimensions)` 校验平台模型的固定维度。向量元数据使用 `model_connection_id`。固定维度位于模型目录，不由租户填写，不代表该连接已经实测；真实输出必须在未来模型接入验证时核对，未确定维度不能绑定向量知识库。连接器被未删除知识库引用时禁止删除。

`../sql/init.sql` 在 UPMS 数据库统一初始化 UPMS、AI、Event 的公共字典、菜单和系统参数；先执行 `../sql/upms.sql`，再执行 `init.sql` 和 `upms_data.sql`。默认租户菜单授权与其他UPMS专属数据统一放在 `upms_data.sql`。脚本不自动执行，也不作为旧环境清理脚本；旧模型配置菜单及其租户关系已从初始化定义中删除。

模型目录的具体模型、Logo及服务商操作链接由开发人员按实际接入范围录入 `ai_model`，本次未虚构可用模型或写入租户专属地址。字典只是分类，增加类型或协议字典项不会自动增加后端适配能力。AI 与 UPMS 可分库，字典值通过稳定编码关联，不建立跨库外键。

## 资源与验证

真实配置 `etd-ai-agent.yml` 明确将 `ai_model` 加入租户及组织忽略表，`ai_model_connection` 仅忽略组织数据权限，保留租户隔离。全局实体通过 `excludeProperty = "tenantId"` 排除基类租户字段，和 UPMS 字典实体一致。

模型目录按服务商、类型及排序建立索引；连接器的模型外键、租户分页和模型筛选有索引支持；知识库连接引用与模型维度外键均有索引支持。关联展示通过 Mapper XML 完成，不在 Java 内存拼装。

部署凭证加密参数使用 `AI_CREDENTIAL_ENCRYPTION_KEY`、`AI_CREDENTIAL_KEY_ID`。

验证命令：

```sh
./mvnw -pl etd-business-parent/etd-business-ai-agent -am -DskipTests compile
./mvnw -pl etd-business-parent/etd-business-ai-agent -am -Dtest='org.etd.ai.**.*Test' -Dsurefire.failIfNoSpecifiedTests=false test
```

聚焦测试覆盖真实应用启动及YAML导入、全局目录共享、连接租户隔离、关联查询、凭证加密及脱敏、显式清空、乐观锁冲突、只读目录授权和旧路由删除。目录模块另有需要 `AI_FOLDER_TEST_JDBC_URL` 的可选 PostgreSQL 测试。
