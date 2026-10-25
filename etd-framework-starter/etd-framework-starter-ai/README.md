# AI Starter

`etd-framework-starter-ai` 是框架统一的 AI 依赖入口，提供 Spring AI Alibaba Agent、Graph，以及 DashScope、OpenAI 兼容、Ollama 底层 SDK。版本沿用 `etd-framework-dependencies` 中的 BOM，业务模块只需引入本 starter。

```xml
<dependency>
    <groupId>org.etd.framework</groupId>
    <artifactId>etd-framework-starter-ai</artifactId>
</dependency>
```

模型能力继续聚合依赖，不创建默认 ChatModel、EmbeddingModel 或 DashScopeAgent，也不加载供应商的模型自动配置。应用启动无需在 YAML 中配置全局 API Key。

SaaS 模型连接、权限校验和凭证持久化由业务模块负责。动态模型入口位于 `org.etd.framework.starter.ai.model`：`ModelClientFactory` 使用协议适配器创建原生 SDK 模型，`ModelClientRegistry` 按租户、连接、配置版本和能力使用有界本地 Map 复用实例。同一缓存键并发只初始化一次，不依赖 JetCache、Caffeine 或 Redis。缓存默认最多 128 条，空闲 10 分钟后在下一次获取时清理；容量满时按最近访问顺序淘汰，不创建定时清理线程。闲置应用可暂留少量过期引用，但条目数始终受容量上限约束。

业务应用实现 `ModelConnectionResolver` 并注册为 Bean 后才自动装配 Registry。每次获取模型都先调用 Resolver 校验可信租户访问并读取最新配置；地址、凭证、能力或模型变化必须递增版本。凭证通过供应器仅在缓存初始化时解密；停用、删除或授权失败必须拒绝获取，不能用缓存命中绕过。各应用节点独立缓存，通过最新配置版本选择实例。框架不访问业务表、不提供默认连接，也不会自动恢复知识库向量化或检索接口。

通过 `getChatModel`、`getEmbeddingModel`、`getRerankModel`、`getImageModel`、`getTranscriptionModel`、`getTextToSpeechModel`、`getModerationModel` 获取对应 SDK 模型，不提供 Instance 或 Lease 包装。请求选项、工具上下文和会话内容由每次调用独立提供，不修改共享默认选项。业务不要自行把模型存入其他长期单例；每次业务执行都通过 Registry 获取，以便重新授权和选择最新配置。淘汰只移除缓存引用，调用方仍持有的模型可继续执行；内置客户端没有需要逐实例关闭的独占资源，共享 HTTP 传输在应用关闭时统一回收。

内置适配器的调用能力如下；这表示框架已接入的 SDK 能力，不是对远端服务器或具体模型的能力探测结果：

| 协议 | 已接入能力 |
| --- | --- |
| OpenAI 兼容 | 聊天、向量、图片生成、语音识别、语音合成（同步/流式）、文本内容审核 |
| DashScope | 聊天、向量、重排 |
| Ollama | 聊天、向量 |

能力代码分别为 `chat`、`embedding`、`rerank`、`image`、`speech_to_text`、`text_to_speech`、`moderation`。OpenAI 兼容服务可能只实现部分端点，须选择已提供对应接口和模型的服务。当前 Spring AI 1.1.2 的图片模型接入生成接口，不包含图片编辑；语音识别为文件转录，不包含 Realtime 实时会话；审核接口当前只接收文本。图片理解以及聊天中的音频输入输出通过 `ChatModel` 的多模态消息和供应商请求选项使用，仍受所选模型限制，不新增独立的图片理解客户端。

语音合成默认沿用 SDK 的 `alloy` 音色和 MP3 格式，可用单次 `OpenAiAudioSpeechOptions` 覆盖；语音识别默认 JSON 响应，可用单次 `OpenAiAudioTranscriptionOptions` 覆盖。审核客户端在请求未指定模型时保留连接的模型配置，避免当前 SDK 将空请求选项转换为供应商默认模型；显式请求模型仍按 SDK 语义覆盖。

其他协议或未接入的能力需要显式实现 `ModelClientProvider`，未知组合直接拒绝，不猜测或切换协议。自定义适配器应返回支持并发的 SDK 模型，不持有需要逐实例关闭的独占资源。向量维度等选项受具体 SDK/模型支持范围约束，当前 Ollama SDK 不提供可设置的向量维度选项。

连接根地址必须包含真实 API 前缀：DashScope 包含 `/api/v1`，OpenAI 兼容接口包含 `/v1` 等实际前缀，Ollama 使用服务根地址。OpenAI 音频和审核 SDK 中固定写入的 `v1` 段会被替换为配置的实际 API 前缀，保留代理路径并避免重复追加版本。工厂创建时不探测连接、不下载模型；SDK 调用不自动重试。无认证时凭证供应器返回 null，不发送 Authorization；API Key 不会进入缓存键或配置快照的 toString。

`etd.ai.model.clients` 可配置节点缓存容量 `maximum-size`（128）、空闲过期 `expire-after-access`（10m）、连接超时 `connect-timeout`（10s）和 HTTP 响应超时 `read-timeout`（60s）。HTTP 传输共用 JDK 连接池，各连接使用独立请求构建器与认证信息。模型、租户地址及凭证均不设置全局默认值。

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
