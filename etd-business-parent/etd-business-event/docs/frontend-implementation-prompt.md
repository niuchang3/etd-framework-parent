# 事件中心前端实现提示词

你正在现有管理后台前端工程中实现“事件中心”功能。请先阅读该前端仓库已有的路由、菜单、权限、请求封装、分页表格、表单弹窗、JSON 查看器、日期格式化和 TypeScript 类型定义，严格复用既有技术栈与页面风格，不要另起一套组件体系。

## 业务目标

事件中心管理四类数据：

1. 事件类型：定义稳定的事件协议编码及当前协议版本。
2. 事件订阅：配置哪个业务应用订阅哪个事件类型，以及事件中心需要投递到的 Kafka Topic 和客户端 Consumer Group。
3. 事件消息：只读查看服务端持久化的原始消息。
4. 投递任务：一条消息针对一个订阅生成一条投递记录。人工重播只操作当前失败投递记录，不能重播该消息的其他订阅，也不能让其他消费组重复收到消息。

## 后端统一约定

- API 前缀：`/v1/event`。
- 统一响应：`{ code, devMessage, message, data, url }`，业务数据读取 `data`。
- MyBatis-Plus 分页数据位于 `data.records`，并同时返回 `total/current/size/pages`。
- 所有时间均为带偏移量的 ISO-8601 时间点，例如 `2026-09-14T18:00:00.000+08:00`。筛选参数原样传递，不要在前端固定增减小时。
- 运行大表后续固定分为 16 片，合法 `shardingKey` 为 `0..15`。进入消息详情或执行投递重播时，必须同时传递列表行中的 `shardingKey` 和 `id`，禁止只传主键。
- 事件类型编码和订阅编码创建后不可修改，编辑表单中应禁用对应输入框。
- 更新事件类型和订阅时必须回传详情中的 `version`，发生并发冲突后提示用户刷新。

## 页面与接口

### 1. 事件类型 `/event/types`

- `GET /v1/event/types`：分页查询。参数：`current`、`size`、`keyword`、`sourceApplication`、`enabled`。
- `GET /v1/event/types/options`：查询启用事件类型，作为订阅表单选项。
- `GET /v1/event/types/{id}`：详情。
- `POST /v1/event/types`：新增，请求字段：`eventType`、`eventName`、`sourceApplication`、`latestVersion`、`description`。
- `PUT /v1/event/types/{id}`：更新，请求体为 `{ "content": {上述字段}, "version": 0 }`。
- `PATCH /v1/event/types/{id}/enabled/{enabled}`：切换状态。
- `DELETE /v1/event/types/{id}`：删除；仍有订阅时后端会拒绝。

列表展示事件名称、事件类型编码、来源应用、最新协议版本、启用状态和更新时间。新增/编辑使用表单弹窗。事件类型编码校验为小写点分格式，例如 `upms.user.created`。

### 2. 事件订阅 `/event/subscriptions`

- `GET /v1/event/subscriptions`：分页查询。参数：`current`、`size`、`keyword`、`eventTypeId`、`subscriberApplication`、`enabled`。
- `GET /v1/event/subscriptions/options`：启用订阅筛选选项。
- `GET /v1/event/subscriptions/{id}`：详情。
- `POST /v1/event/subscriptions`：新增，请求字段：`subscriptionCode`、`subscriptionName`、`eventTypeId`、`subscriberApplication`、`targetTopic`、`consumerGroup`、`description`。
- `PUT /v1/event/subscriptions/{id}`：更新，请求体为 `{ "content": {上述字段}, "version": 0 }`。
- `PATCH /v1/event/subscriptions/{id}/enabled/{enabled}`：切换状态。
- `DELETE /v1/event/subscriptions/{id}`：逻辑删除。

列表同时展示事件名称/编码、订阅应用、Kafka Topic、Consumer Group、状态和更新时间。事件类型必须使用 options 接口选择，不允许自由填写 ID。增加说明提示：同一业务应用的多个实例使用同一个 Consumer Group；不同业务应用使用不同订阅和消费组。

### 3. 事件消息 `/event/messages`

- `GET /v1/event/messages`：分页查询。参数：`current`、`size`、精确匹配的 `eventId`、`eventTypeId`、`sourceApplication`、`startTime`、`endTime`。
- `GET /v1/event/messages/{shardingKey}/{id}`：聚合详情，返回：
  - `message`：原始消息、上下文和业务 JSON 载荷；
  - `eventType`：事件类型定义；
  - `deliveryList`：该消息面向每个业务订阅的独立投递结果。

列表不要展示大 JSON。点击详情后使用抽屉或详情页展示基础信息，并使用安全的只读 JSON 查看器格式化 `eventContext`、`eventPayload`；下方展示投递结果表。不要提供修改或删除消息的按钮。

### 4. 投递管理 `/event/deliveries`

- `GET /v1/event/deliveries`：分页查询。参数：`current`、`size`、精确匹配的 `eventId`、`subscriptionId`、`deliveryStatus`、`startTime`、`endTime`。
- `GET /v1/event/deliveries/{shardingKey}/{id}`：投递详情。
- `POST /v1/event/deliveries/{shardingKey}/{id}/replay`：定向重播当前失败订阅。

投递状态映射必须集中定义：`0 待投递`、`1 投递中`、`2 成功`、`3 等待重试`、`4 死信`。仅状态 `3`、`4` 展示“重播”按钮。重播前弹出二次确认，明确显示事件 ID、订阅名称、订阅应用和目标 Topic；成功后刷新当前列表与详情。不要实现“整条消息全部重播”或批量重播。

## 交互和工程要求

- API、类型、页面、路由按前端现有领域目录组织，避免把所有代码写进一个页面文件。
- 搜索表单支持重置；切换分页或筛选时保持请求竞态安全。
- 状态切换、删除、重播均需要二次确认和明确的成功/失败反馈。
- 权限资源码只使用 `event:type`、`event:subscription`、`event:message`、`event:delivery`，不要自创 read/write 后缀。按钮读写权限沿用项目已有访问级别机制。
- 事件消息载荷可能很大，列表接口返回的 `eventContext`、`eventPayload` 为空是正常行为，只有详情接口包含完整 JSON。
- 为 API 参数、响应模型、状态映射和关键页面交互补充必要测试。

## 必须返回的菜单配置

完成代码后，你的答复必须单独给出“菜单配置”章节，并依据当前前端项目真实菜单类型输出可直接落库或注册的配置。如果项目菜单由后端数据库维护，至少返回下面字段对应的 SQL 或 JSON；如果由前端静态路由维护，返回等价的路由配置。菜单层级必须是：

```json
{
  "menuName": "事件中心",
  "menuPath": "/event",
  "menuRouter": null,
  "menuIcon": "NotificationOutlined",
  "menuType": "DIRECTORY",
  "sort": 50,
  "permissionCode": null,
  "children": [
    {
      "menuName": "事件类型",
      "menuPath": "/event/types",
      "menuRouter": "@/views/event/types/index.vue",
      "menuIcon": "TagsOutlined",
      "menuType": "MENU",
      "sort": 10,
      "permissionCode": "event:type"
    },
    {
      "menuName": "事件订阅",
      "menuPath": "/event/subscriptions",
      "menuRouter": "@/views/event/subscriptions/index.vue",
      "menuIcon": "ApiOutlined",
      "menuType": "MENU",
      "sort": 20,
      "permissionCode": "event:subscription"
    },
    {
      "menuName": "事件消息",
      "menuPath": "/event/messages",
      "menuRouter": "@/views/event/messages/index.vue",
      "menuIcon": "MessageOutlined",
      "menuType": "MENU",
      "sort": 30,
      "permissionCode": "event:message"
    },
    {
      "menuName": "投递管理",
      "menuPath": "/event/deliveries",
      "menuRouter": "@/views/event/deliveries/index.vue",
      "menuIcon": "SendOutlined",
      "menuType": "MENU",
      "sort": 40,
      "permissionCode": "event:delivery"
    }
  ]
}
```

最后请返回：修改文件清单、页面能力说明、接口对应关系、菜单配置、实际执行的 lint/type-check/test 命令及结果。不要只给示例代码，要在当前前端仓库中完成实现。
