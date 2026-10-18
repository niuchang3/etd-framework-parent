# AI 基础管理服务

入口 `org.etd.ai.AgentApplication`，默认端口8300，Java21 / Spring Boot3.5，依赖版本沿用框架BOM。

当前实现租户模型连接与模型草稿、知识库配置、多级目录、私有MinIO原件分片上传、替换、下载、元数据维护和删除。目录与文件使用“租户内公开 OR 现有角色数据范围”，写操作不会因公开标记而扩大权限。分片传输使用持久化任务，支持幂等完成确认、取消、过期回收及原件清理重试。

目录与文档读取统一通过Service方法上的 `@DataPermission(tables = ...)` 声明公开字段，框架负责在SQL中追加角色范围；Mapper XML不再维护 `readScope`。目录查询分别收集可读目录和可读文件所在目录，再递归补齐导航祖先，祖先不因此获得管理权限。循环检查、目录非空检查及已授权后的投影清理仍使用其原有业务边界。

**不调用模型，不执行文档解析、文本分片、向量化或检索。**AI 依赖由 `etd-framework-starter-ai` 统一提供，使用底层模型 SDK，不引入 DashScope 模型自动配置，启动和文件管理不需要全局 DashScope API Key。页面保存的模型均为未验证草稿，实测维度当前为空且页面不可编辑；知识库模型绑定和模型测试尚未开放。

## 初始化与运行

1. 创建AI数据库（本地默认 `ai-agent`），PostgreSQL16服务需已安装vector和zhparser扩展文件。
2. 连接AI库依次显式执行 `src/main/resources/sql/postgresql/extensions.sql`、`schema.sql`。schema为最终首次建库结构，不是增量迁移脚本，不可在已有同名表的库里直接重复运行。
3. 连接UPMS系统表所在库执行本模块的 `src/main/resources/sql/postgresql/init.sql`，前提是已完成UPMS建表。本模块不复制sys_dict/sys_config表、不在启动时自动执行初始化脚本。
4. 准备与UPMS一致的Redis登录状态存储、RSA验签公钥，以及私有MinIO。浏览器直传需额外配置前端Origin的CORS及ETag暴露。
5. 目录、上传新文件要求用户已配置主组织。AI接口使用框架@Permission校验read/write，注解直接填写资源码，目录与文件继续叠加框架数据权限。

在仓库根目录：

```bash
./mvnw -pl etd-business-parent/etd-business-ai-agent -am -DskipTests package
java -jar etd-business-parent/etd-business-ai-agent/target/etd-business-ai-agent-0.0.1-SNAPSHOT.jar
```

共享组件配置由父POM将 `etd-business-parent/config/` 打包到classpath根目录。application.yml只负责入口及导入；应用策略在etd-ai-agent.yml，本地连接信息在application-local.yml。租户模型连接由数据库管理，不通过公共 YAML 配置全局模型凭证。

| 环境变量 | 本地默认值/作用 |
| --- | --- |
| AI_AGENT_PORT | 8300 |
| DB_HOST / DB_PORT / DB_NAME | 127.0.0.1 / 30432 / ai-agent |
| DB_USERNAME / DB_PASSWORD | root / 123456，仅本地学习 |
| REDIS_* | 复用父级redis.yml，必须与UPMS登录状态存储一致 |
| AI_SECURITY_PUBLIC_KEY_PATH | conf/rsaPublicKey.pem，使用UPMS对应公钥 |
| AI_STORAGE_MINIO_ENDPOINT | http://localhost:30900，浏览器与服务端都必须可达的S3 API地址 |
| AI_STORAGE_MINIO_ACCESS_KEY / AI_STORAGE_MINIO_SECRET_KEY | root / 12345678，仅本地学习 |
| AI_STORAGE_MINIO_BUCKET | ai-agent |
| AI_CREDENTIAL_ENCRYPTION_KEY | local环境提供固定开发默认值；其他环境必须注入Base64编码的随机32字节AES主密钥 |
| AI_CREDENTIAL_KEY_ID | local默认ai-local-01；其他环境必须注入与主密钥配套的标识 |

凭证使用AES-GCM随机nonce，并以租户和连接ID绑定AAD。local环境提供固定开发密钥，支持直接保存API Key；该公开默认值仅用于本地开发。其他环境必须注入独立主密钥并稳定保管，不能在每次重启时重新生成，也不能提交到仓库。当前没有主密钥轮换/旧密钥兼容链。未配置时无认证连接仍可维护，但拒绝保存API Key。所有接口均不回显凭证明文或密文。

## 接口与前端交付

可以直接交给前端AI的完整提示词：[frontend-ai-prompt.md](docs/frontend-ai-prompt.md)。包含全部实际接口、请求响应、五类功能页面、六种字典、两个上传系统参数，以及必须返回的菜单清单格式。

- [知识库结构及权限方案](docs/knowledge-base-schema.md)
- [文件上传组件约定](docs/file-upload-component.md)
- [客户端上传校验实施说明](docs/frontend-upload-client-validation.md)
- [租户模型结构](docs/tenant-model-schema.md)
- [本轮前端增量：保留框架接口权限](docs/frontend-permission-resource.md)

除了 `/actuator/health`，所有AI业务接口都需要UPMS Bearer令牌，并校验对应read/write authority；数据访问继续由框架租户及组织/公开规则控制。统一业务成功码2000，HTTP200不必然成功。平台管理员按最高系统权限处理，XML不为其额外追加租户或组织范围限制。

上传参数仍在sys_config维护：`ai-agent-file-type`、`ai-agent-file-upload`，由客户端直接读取并校验文件类型、文件大小和参数有效性。AI后端不访问UPMS参数接口、不配置参数服务地址；客户端在初始化请求中提交partSizeBytes、concurrency、maxRetries、urlExpirySeconds、sessionExpireSeconds。后端仅保留鉴权、S3协议边界、实际分片及总字节数一致性检查和SHA256计算，不执行格式或编码限制。详见[客户端上传校验实施说明](docs/frontend-upload-client-validation.md)。

## 验证

上传参数改由客户端提交后的聚焦回归（外部数据库及MinIO使用替身）：

```bash
./mvnw -pl etd-business-parent/etd-business-ai-agent -am -Dtest=UploadClientParametersTest,UploadedFileInspectorTest,AiEndpointAuthenticationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

测试覆盖客户端参数透传、旧文件类型和大小限制移除、S3技术边界、摘要与字节数核对，以及既有接口认证。真实MinIO上传、浏览器CORS及前端参数校验需要按客户端实施说明联调，单元测试不代表这些场景已经验证。

## 注解读取权限回归

`KnowledgeReadPermissionIntegrationTest` 在加载当前 `extensions.sql` 和 `schema.sql` 的空PostgreSQL测试库中执行。必须使用一次性数据库，设置 `AI_PERMISSION_TEST_DB_URL`、`AI_PERMISSION_TEST_DB_USER`、`AI_PERMISSION_TEST_DB_PASSWORD` 后运行：

```bash
./mvnw -pl etd-business-parent/etd-business-ai-agent -am -Dtest=KnowledgeReadPermissionIntegrationTest,PublicReadDataPermissionTest,EtdDataPermissionHandlerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

未设置测试库URL时该数据库用例明确跳过。测试通过真实Mapper XML及Spring权限切面验证分页总数、详情、业务筛选、公开/本人/部门范围、跨租户隔离、私有祖先导航及读写权限边界，不依赖MinIO或模型服务。

## XML租户与逻辑删除过滤

AI应用已开启 `etd.mybatis.logic-delete.enabled`。普通文档、目录及模型引用查询由框架统一追加租户和逻辑删除条件，XML保留知识库、状态、名称等业务筛选，不再额外维护表间租户等值条件，不再传递仅用于普通查询过滤的租户参数。平台管理员遵循框架跨租户规则，但仍过滤已删除资料。

目录非空检查使用 `UNION ALL` CTE汇总，使两个分支都经过租户及逻辑删除过滤；检查仍包含当前用户无读取权限的有效子项。后台跨租户清理及已授权后的物理投影清理按全局任务ID或文档ID定位，普通调用的租户隔离仍由框架负责。

## 管理页面状态筛选

五个管理列表统一支持可选`dataStatus`：不传查看全部，`0`禁用，`1`启用；响应VO同步返回该字段。文档管理查询不再固定要求文档或知识库启用，目录树筛选保留必要祖先。原件下载/预览继续由后端校验知识库与文件启用状态。前端增量对接要求见`docs/frontend-ai-prompt.md`末尾，复用现有`common_status`字典，无新增字典、系统参数或菜单权限。

## Service层权限与版本处理

普通Service查询与更新的租户、组织范围统一由MyBatis拦截器处理；平台管理员不再被手写当前租户条件限制。原 `KnowledgeScope` 已移除，页面writable标记改用框架 `DataPermissionScope`，组织资料新增策略单独维护在 `KnowledgeCreationPolicy`。

管理实体及上传会话通过 `@Version` 使用原生乐观锁；业务不再手工比较数据库version或递增并发版本，更新仍检查影响行数。目录parentId和文档folderId明确支持更新为空，审计字段由框架填充。上传替换快照、处理轮次、任务状态CAS及目录结构校验保留。

模型连接跨租户编辑凭证使用已存记录的tenantId参与加密，避免平台管理员当前上下文导致密文租户绑定错误。前端继续回传读取时的version，接口字段和更新成功响应保持原有约定；更新后重新取详情或刷新列表获取新版本，无需新增接口或菜单。

## AI接口认证回归

AI Controller直接使用框架@Permission声明资源码，不保留AiPermission常量类。下面的离线测试复用生产Security过滤链与方法鉴权，检查匿名业务请求返回401、健康白名单、无权限用户返回403，以及read/write权限边界：

```bash
./mvnw -pl etd-business-parent/etd-business-ai-agent -am -Dtest=AiEndpointAuthenticationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

前端按 `docs/frontend-permission-resource.md` 核对是否需要恢复上一轮移除的权限判断；目录、文件的writable约束及框架租户/数据权限继续生效。已有菜单数据不会被本次源码修改自动覆盖。
