# AI Starter

`etd-framework-starter-ai` 是框架统一的 AI 依赖入口，提供 Spring AI Alibaba Agent、Graph 和 DashScope 底层 SDK。版本沿用 `etd-framework-dependencies` 中的 BOM，业务模块只需引入本 starter。

```xml
<dependency>
    <groupId>org.etd.framework</groupId>
    <artifactId>etd-framework-starter-ai</artifactId>
</dependency>
```

当前只聚合依赖，不创建默认 ChatModel、EmbeddingModel 或 DashScopeAgent，也不加载供应商的模型自动配置。应用启动无需在 YAML 中配置全局 API Key。

SaaS 模型连接、权限校验和凭证持久化由业务模块负责。后续模型客户端应根据已授权的租户连接配置在运行时创建，并提供给对应的 Agent 或工作流执行；当前尚未实现客户端工厂、客户端缓存或模型调用。

本阶段没有自定义自动配置类，因此无需注册 `AutoConfiguration.imports`；新增自动配置能力时必须按 Spring Boot 3 的方式注册。

验证无全局模型凭证时的自动配置启动：

```bash
./mvnw -pl etd-framework-starter/etd-framework-starter-ai -am -Dtest=AiStarterStartupTest -Dsurefire.failIfNoSpecifiedTests=false test
```
