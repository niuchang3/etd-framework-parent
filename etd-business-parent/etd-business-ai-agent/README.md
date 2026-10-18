# AI Agent 业务应用

当前保留模型连接、模型配置和独立目录管理能力。知识库、文档、分片、向量及其上传和后台清理实现已移除，等待重新设计。

## 接口与菜单

- 模型连接：`/v1/model-connections`，资源码 `ai:model-connection`。
- 模型配置：`/v1/model-configs`，资源码 `ai:model-config`。
- 独立目录：`/v1/knowledge-folders`，资源码 `ai:knowledge-folder`。

业务接口使用框架身份认证、功能权限和日志能力，目录按前端指定的 `orgId` 分组，AI 服务不查询 UPMS 校验部门存在性或授权范围，普通用户遵循框架角色数据范围，租户管理员跳过数据范围但保留租户隔离，平台管理员沿用框架全局管理权限。

目录列表的 `orgId` 可选，不传时查询当前数据权限范围内全部目录；新增/编辑仍必传，响应包含目录所属 `orgId`。父子目录必须同部门，不支持跨部门移动目录。

目录新增、编辑和列表响应支持 `description`（选填，最多500字）；更新传 `null` 可清空描述。

## 数据库初始化

`src/main/resources/sql/postgresql/schema.sql` 和 `extensions.sql` 保留完整建表及扩展定义，其中包括待重新设计功能的表；保留表结构不代表提供对应业务接口。

`src/main/resources/sql/postgresql/init.sql` 在 UPMS 系统表所在数据库手动执行，仅初始化保留能力所需的模型字典、菜单和默认租户菜单关系，不随应用启动执行。

## 运行配置

应用从共享配置导入 Redis、数据源连接池，并加载自身的 `etd-ai-agent.yml`。模型凭证由后端加密保存，部署环境通过 `AI_CREDENTIAL_ENCRYPTION_KEY` 和 `AI_CREDENTIAL_KEY_ID` 注入加密配置。AI SDK 由框架 AI starter 提供。
