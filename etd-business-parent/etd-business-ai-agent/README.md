# AI Agent 业务应用

平台统一管理全局模型，租户选择模型后创建连接器。服务商、模型类型、接入协议与认证方式使用 UPMS 全局字典。目录忽略租户和组织隔离，连接器保留租户隔离。

## 数据与接口

- `ai_model`：平台管理员通过模型管理接口维护的模型信息，无 `tenant_id`；模型代码、Logo、密钥获取链接和接入指南在此维护。`GET /v1/models` 支持 `providerCode`、`modelType`、`dataStatus`、`current`、`size`；`GET /v1/models/{id}` 返回详情。资源码 `ai:model`。`POST /v1/models` 新增，`PUT /v1/models/{id}` 按版本修改，`DELETE /v1/models/{id}` 逻辑删除；仅平台管理员允许写，普通租户只读已启用记录，管理员可查询停用记录。
- `ai_model_connection`：一条连接器对应一个平台模型，保存 `model_id`、`protocol`、实际服务地址与加密凭证，没有连接名称。`/v1/model-connections` 提供 GET/POST，`/{id}` 提供 GET/PUT/DELETE。资源码 `ai:model-connection`。分页支持 `modelId`、`dataStatus`、`current`、`size`，响应通过 SQL 关联返回模型名称、类型、Logo 和服务商。
- 独立目录：`/v1/knowledge-folders`，资源码 `ai:knowledge-folder`，原部门归属和数据权限语义不变。

模型管理通过服务商字典值分类；一个服务商下有效模型代码唯一，不同服务商可收录相同模型。不从服务商品牌推断协议；租户在连接器中选择协议，当前可保存 `dashscope`、`ollama`、`openai_compatible`。保存配置不表示完成了连通性测试，也不表示所有类型的模型调用均已实现。本次不新增模型调用、模型发现或探测接口。

连接器允许同一租户为同一模型建立多条记录，通过模型、协议、地址及记录ID识别。创建后禁止切换绑定模型，换模型应重新建立连接。API Key 只写，新建 `api_key` 认证必须提供凭证；更新留空保留原密文，切换 `none` 清空密文和密钥标识。更新必须提交当前 `version`。DTO JSON序列化及toString均不输出API Key；响应不输出密文、加密标识或租户身份。

平台目录停用后不再列入可选项，也不允许新增或编辑连接；已建立连接仍可查询或删除。平台管理员可以通过目录接口维护模型。被任意租户有效连接引用的模型不能删除，也不能修改服务商、调用代码、主能力类型；展示信息和启停状态可以维护。

## 数据库初始化

`../etd-business-common/sql/ai.sql` 定义模型、模型连接器、知识库目录及资料上传表，由部署人员显式执行，不随应用启动执行。知识资料当前仅维护原件上传、基本信息和列表展示；不提供正文解析、分片预览、向量化或检索发布能力，也不创建对应表和检索扩展。

`../etd-business-common/sql/init.sql` 在 UPMS 数据库统一初始化 UPMS、AI、Event 的公共字典、菜单和系统参数；先执行 `../etd-business-common/sql/upms.sql`，再执行 `init.sql` 和 `upms_data.sql`。默认租户菜单授权与其他UPMS专属数据统一放在 `upms_data.sql`。脚本不自动执行，也不作为旧环境清理脚本；旧模型配置菜单及其租户关系已从初始化定义中删除。

`../etd-business-common/sql/ai_data.sql` 提供经官方文档核实的12条模型初始数据，在AI数据库完成 `ai.sql` 后执行；后续可通过模型管理接口维护。Logo使用已上传本地MinIO的完整图片地址，其他环境初始化前按实际对象存储地址调整；不写入租户专属凭证。字典只是分类，增加类型或协议字典项不会自动增加后端适配能力。AI 与 UPMS 可分库，字典值通过稳定编码关联，不建立跨库外键。

## 资源与验证

真实配置 `etd-ai-agent.yml` 明确将 `ai_model` 加入租户及组织忽略表，`ai_model_connection` 仅忽略组织数据权限，保留租户隔离。全局实体通过 `excludeProperty = "tenantId"` 排除基类租户字段，和 UPMS 字典实体一致。

模型管理按服务商、类型及排序建立索引；连接器的模型关联查询、租户分页和模型筛选有索引支持；资料目录查询及上传过期清理保留必要的业务查询索引。关联展示通过 Mapper XML 完成，不在 Java 内存拼装。

部署凭证加密参数使用 `AI_CREDENTIAL_ENCRYPTION_KEY`、`AI_CREDENTIAL_KEY_ID`。

验证命令：

```sh
./mvnw -pl etd-business-parent/etd-business-ai-agent -am -DskipTests compile
./mvnw -pl etd-business-parent/etd-business-ai-agent -am -Dtest='org.etd.ai.**.*Test' -Dsurefire.failIfNoSpecifiedTests=false test
```

聚焦测试覆盖真实应用启动及YAML导入、全局目录共享、连接租户隔离、关联查询、凭证加密及脱敏、显式清空、乐观锁冲突、模型管理管理员写权限与普通租户只读授权和旧路由删除。目录模块另有需要 `AI_FOLDER_TEST_JDBC_URL` 的可选 PostgreSQL 测试。
