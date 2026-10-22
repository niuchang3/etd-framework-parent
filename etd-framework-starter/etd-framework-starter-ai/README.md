# AI Starter

`etd-framework-starter-ai` 是框架统一的 AI 依赖入口，提供 Spring AI Alibaba Agent、Graph 和 DashScope 底层 SDK。版本沿用 `etd-framework-dependencies` 中的 BOM，业务模块只需引入本 starter。

```xml
<dependency>
    <groupId>org.etd.framework</groupId>
    <artifactId>etd-framework-starter-ai</artifactId>
</dependency>
```

模型能力继续聚合依赖，不创建默认 ChatModel、EmbeddingModel 或 DashScopeAgent，也不加载供应商的模型自动配置。应用启动无需在 YAML 中配置全局 API Key。

SaaS 模型连接、权限校验和凭证持久化由业务模块负责。后续模型客户端应根据已授权的租户连接配置在运行时创建，并提供给对应的 Agent 或工作流执行；当前尚未实现客户端工厂、客户端缓存或模型调用。

文档解析自动配置通过 `AutoConfiguration.imports` 注册，默认关闭 Docling。公共入口位于 `org.etd.framework.starter.ai.document`：

- `DocumentParserRegistry` 按引擎代码选择实现；`AsynchronousDocumentParser` 提供提交、单次状态查询和结果获取。
- `DocumentChunker` 提供同步 `chunk`、异步 `submitChunk` 和 `fetchChunkResult`，任务状态通过 `fetchStatus` 单次查询。能力集合 `chunkStrategies` 明确列出引擎支持的分块策略。
- `DoclingDocumentParser` 使用官方 Java SDK 0.7.0，通过 v1 API 调用独立部署的 Docling Serve，不在 JVM 内运行解析模型。
- 结果包含 Markdown、按正文树排序的内容块、表格结构、页码和位置；坐标在尺寸完整时转换为左上角原点，原始 JSON 仅供引擎扩展和诊断。
- 任务完成与文档完整成功分别表达；部分成功、跳过和失败由调用方显式处理。

启用时配置 `etd.ai.document.docling.enabled=true` 和 `etd.ai.document.docling.base-url`；认证通过 `etd.ai.document.docling.api-key` 从环境注入。连接超时默认 10 秒，请求超时默认 60 秒，`max-file-bytes` 默认 20 MiB。解析选项由每次请求提供，文档处理超时与 HTTP 请求超时分别设置。服务地址和凭证不提供默认值，客户端创建不发送探测请求。

SDK 以 Base64 JSON 传输文件，因此本适配器按实际读取字节数限制大小；图片使用占位方式，不内嵌图片。复用单个 SDK 客户端及其 HTTP 连接管理，不自动重试提交、不启动轮询、不持久化任务。调用方负责文件及任务授权、任务恢复、结果保存、分块策略选择和向量化。请求、产物和原始异常可能包含敏感内容，不应直接记录日志或返回前端。本次未接入业务模块；真实引擎效果需在部署锁定版本的 Docling Serve 后验证。

原生分块使用 `DocumentChunkRequest`，包含原文件解析请求、类型化分块选项和 `includeConvertedDocument`：

- `HierarchicalDocumentChunkOptions` 按原文结构分块，暴露 SDK 支持的 `useMarkdownTables` 和 `includeRawText`。
- `HybridDocumentChunkOptions` 额外暴露 `maxTokens`、`tokenizer` 和 `mergePeers`。可空参数省略后由服务决定，显式空白 tokenizer 被拒绝；tokenizer 应与向量模型匹配，并由 Docling 服务加载。
- `DocumentChunk` 保留上下文文本、原始文本、Token 数、标题路径、说明、原文内容块引用、页码和元数据。Token 数可能为空，不能用字符数代替。
- `includeConvertedDocument=true` 时附带原文结构、逐文档状态和诊断信息；关闭时 `documents` 为空，不据此推断文档完整成功。异步任务结果使用 `fetchChunkResult`，不能与普通解析的 `fetchResult` 混用。
- 同步调用受单次 HTTP 超时约束，大文件应使用异步任务。框架不自动轮询，不重复实现原生分块算法，也不暴露当前 SDK 未提供的参数。

可显式设置测试环境变量 `ETD_TEST_DOCLING_BASE_URL` 运行 `DoclingChunkLiveTest`，需要认证时再提供 `ETD_TEST_DOCLING_API_KEY`。测试使用自带的小型 Markdown 数据验证真实同步与异步分块；默认不连接真实服务。

验证无全局模型凭证时的自动配置启动：

```bash
./mvnw -pl etd-framework-starter/etd-framework-starter-ai -am -Dtest=AiStarterStartupTest -Dsurefire.failIfNoSpecifiedTests=false test
```
