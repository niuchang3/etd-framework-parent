# 智能体业务应用

`etd-business-ai-agent` 是独立 Spring Boot 应用，入口为 `org.etd.ai.AgentApplication`，默认端口为 `8300`。

## 已接入能力

- Spring AI Alibaba Agent Framework：智能体与多智能体编排。
- Spring AI Alibaba Graph Core：图工作流运行时。
- DashScope：百炼对话模型，默认 `qwen-plus`，通过 `AI_DASHSCOPE_MODEL` 调整。
- 框架 Web、日志、参数校验、HTTP 时间序列化与 MinIO / OSS 文件存储。
- 框架 MyBatis Plus、Druid 与 PostgreSQL JDBC 驱动；按功能域扫描 `org.etd.ai.**.mapper`。
- 框架 Security 客户端：验证 UPMS 签发的 Bearer 访问令牌，并通过 Redis 校验登录状态。
- 框架 Redis / JetCache 缓存、租户隔离、组织数据权限与实体字段自动填充。
- Actuator：仅暴露 `/actuator/health`，不输出健康详情。

版本统一由 `etd-framework-dependencies` 管理，导入官方 `spring-ai-alibaba-bom`、`spring-ai-bom` 和 `spring-ai-alibaba-extensions-bom`。版本组合依据[官方版本说明](https://java2ai.com/docs/versions/)，保留项目 Java 21 / Spring Boot 3.5 基线。

BOM 统一管理依赖版本，不会自动引入所有实现。目前不接入 Nacos、MCP 服务端、VectorStore 或事件客户端；实际业务需要时再添加对应能力。本模块当前只提供应用骨架，尚未提供聊天、文件上传等业务 HTTP 接口。PostgreSQL 已接入，但尚未创建向量表或启用 Embedding / pgvector 检索；这些需要按后续确定的模型维度、数据隔离和索引方案配置。

## 编译与启动

在仓库根目录执行：

```bash
./mvnw -pl etd-business-parent/etd-business-ai-agent -am -DskipTests package
```

配置通过父 POM 打包公共资源：`application.yml` 导入 `classpath:redis.yml` 和 `classpath:druid.yml`，默认启用 `local`；`application-local.yml` 保存 AI 独立数据库、模型和 MinIO 连接信息。环境变量未设置时使用本地默认值；真实模型凭证仍须注入。

| 环境变量 | 本地默认值 / 用途 |
| --- | --- |
| `AI_DASHSCOPE_API_KEY` | 必填的百炼模型凭证，无默认值 |
| `AI_DB_HOST`、`AI_DB_PORT`、`AI_DB_NAME` | `127.0.0.1`、`30432`、`ai_agent`，数据库需提前创建 |
| `AI_DB_USERNAME`、`AI_DB_PASSWORD` | `root`、`123456`，与 UPMS 本地 PostgreSQL 保持一致 |
| `REDIS_HOST`、`REDIS_PORT` | `127.0.0.1`、`30637`，公共配置沿用逻辑库 `4`，需与 UPMS 登录令牌所在实例和逻辑库一致 |
| `REDIS_PASSWORD` | `123456`，与 UPMS 本地 Redis 保持一致 |
| `AI_SECURITY_PUBLIC_KEY_PATH` | `conf/rsaPublicKey.pem`，复用 UPMS 的 RSA 公钥；支持绝对路径或 `classpath:` |

本地 PostgreSQL 默认地址为 `jdbc:postgresql://127.0.0.1:30432/ai_agent`；请提前创建 `ai_agent` 数据库。若本机无法通过 localhost 访问 Kubernetes NodePort，可覆盖数据库、Redis 的主机和端口。无密码 Redis 环境应在对应环境文件中省略 password 配置。公钥必须来自令牌签发端，AI 服务只负责验签，无需配置私钥。

```bash
java -jar etd-business-parent/etd-business-ai-agent/target/etd-business-ai-agent-0.0.1-SNAPSHOT.jar
```

`AI_AGENT_PORT` 可覆盖监听端口。健康检查包含数据库与 Redis 的连通性，不代表模型调用或文件读写已经通过外部服务验证。

需要其他运行环境时，新增对应的 `application-{profile}.yml` 并通过 `--spring.profiles.active=<环境名>` 选择；本地默认连接和账号密码只放在 local 文件中。

## 接口鉴权与数据访问

只有 `/actuator/health` 允许匿名访问，其他路径必须提供 `Authorization: Bearer <UPMS访问令牌>`。客户端不提供登录或令牌签发接口。后续业务 Controller 使用框架 `@Permission` 声明资源权限，用户需在 UPMS 获授对应读写权限；权限变化的生效时间遵循 UPMS 既有的令牌更新和撤销机制。

MyBatis 复用框架的租户、组织数据权限拦截器和自动填充。业务实体包含组织权限时继承 `OrgScopedEntity`，其他实体按实际隔离范围选择基类；Mapper XML 按功能域放到 `resources/mapper/<功能域>`。本模块不引入未使用的 ShardingSphere、MySQL 或运行时 H2，也不会在启动时自动建表。

以后接入 Spring AI PgVectorStore 时，需要单独设计租户、组织和文档权限过滤；框架 MyBatis 拦截器不能自动保护绕过 MyBatis 的 JDBC / VectorStore 查询。

## 文件存储

本地 `local` 环境默认启用 MinIO，无需再指定存储厂商 profile。存储开关、私有访问策略和连接信息统一放在 `application-local.yml`，公共配置不再重复声明；未配置的存储厂商由框架默认保持关闭。

| MinIO 环境变量 | 未设置时的默认值 |
| --- | --- |
| `AI_STORAGE_MINIO_ENDPOINT` | `http://minio-service.default.svc.cluster.local:9000` |
| `AI_STORAGE_MINIO_ACCESS_KEY` | `root` |
| `AI_STORAGE_MINIO_SECRET_KEY` | `12345678` |
| `AI_STORAGE_MINIO_BUCKET` | `ai-agent` |

默认连接对应当前 OrbStack 本地学习环境，账号密码与 MinIO Deployment 的默认值一致。如果服务端通过 Secret 修改了账号密码，这里也需用环境变量同步覆盖。其他 Kubernetes 环境或命名空间通过 `AI_STORAGE_MINIO_ENDPOINT` 指定可访问的 S3 API 地址。环境变量未设置时才使用默认值，显式设置为空字符串不会触发默认值。

显式选择本地环境启动（也可省略该参数，默认已经是 local）：

```bash
java -jar etd-business-parent/etd-business-ai-agent/target/etd-business-ai-agent-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

启用后可在实际业务 Service 中注入框架的 `FileUpload` / `FileDownload`。文件使用私有访问策略；上传、下载和预签名接口的完整说明见[存储 starter 文档](../../etd-framework-starter/etd-framework-starter-storage/README.md)。后续业务按 `org.etd.ai.<功能域>.<技术分层>` 组织。

## 验证

```bash
./mvnw -pl etd-business-parent/etd-business-ai-agent -am test
./mvnw -pl etd-business-parent/etd-business-ai-agent -am -DskipTests dependency:tree
```

集成冒烟测试使用运行时生成的测试密钥，启动真实 Web / Security 上下文并初始化 DashScope、Agent、MinIO、Druid 和 MyBatis。测试使用仅在 test scope 引入的 H2 隔离数据库连接，模拟 Redis 登录令牌存储并关闭 Redis 健康探测，不请求外部服务；覆盖健康检查匿名访问、业务接口匿名拒绝、无效令牌、缺少权限、正确授权和令牌撤销。测试中的探针 Controller 不会进入正式应用。

真实 PostgreSQL / Redis 连通性、UPMS 联调、模型调用与存储读写需在对应运行环境中验证。H2 测试不代表 PostgreSQL 或向量检索已完成验证。
