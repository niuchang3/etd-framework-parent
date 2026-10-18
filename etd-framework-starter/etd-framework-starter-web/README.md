# Web Starter

引入 `etd-framework-starter-web` 的 Servlet 应用使用内嵌 Tomcat 时，请求头大小上限默认统一为 64KB，无需在每个应用中重复配置。

应用可以通过 Spring Boot 原生配置覆盖默认值：

```yaml
server:
  max-http-request-header-size: 128KB # 当前应用确需更大请求头时显式调整。
```

环境变量 `SERVER_MAX_HTTP_REQUEST_HEADER_SIZE` 和启动参数 `--server.max-http-request-header-size=128KB` 同样有效。显式配置可以大于或小于框架默认值。

此默认值只调整内嵌 Tomcat 的请求头上限，不调整响应头、请求体或网关的限制，也不作用于外置 Servlet 容器。
