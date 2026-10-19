# 给前端 AI 的重构任务

请按以下已落地的后端契约重构 AI 模块，直接删除旧实现，不做任何兼容、路由重定向、字段别名或双套流程。

## 页面与菜单

1. 删除原“模型配置”页面、API封装、类型定义、表单、路由及权限引用：`/ai/model-configs`、`@/views/ai/model-config/index.vue`、`ai:model-config`。旧 `/v1/model-configs` 全部接口已经删除。
2. 新增只读“模型目录”：`/ai/models`，组件 `@/views/ai/model/index.vue`，权限资源 `ai:model`。按模型卡片展示名称、Logo、服务商、类型、介绍，可按服务商及模型类型组合筛选。开发人员维护目录，租户端不提供新增、修改或删除模型按钮。
3. “模型连接”改为“模型连接器”，保留路径 `/ai/model-connections`、组件 `@/views/ai/model-connection/index.vue`、权限资源 `ai:model-connection`。删除连接器名称、旧 `provider` 字段、备注、手填模型名称/类型/调用代码/向量维度等旧表单及提交字段。
4. 知识库分类页面、接口及组织数据权限交互保持不变。完整菜单字段见同目录 `ai-menus.json`。

## 模型目录接口

- `GET /v1/models?current=1&size=10&providerCode=aliyun&modelType=chat`，服务商和类型可省略，分页size最大200。仅返回启用且未删除模型。
- `GET /v1/models/{id}`，取得已启用模型详情和接入指引。
- 单条字段：`id`、`providerCode`、`modelName`、`modelCode`、`modelType`、`logoPath`、`description`、`apiKeyUrl`、`setupGuideUrl`、`baseUrlHelp`、`embeddingDimensions`、`sort`。名称、代码、维度及指引均为只读，不让租户维护。
- 不提供模型目录写接口。`modelCode`用于后端调用，`embeddingDimensions`为平台定义维度，不表示连接已经验证。

## 字典

沿用项目现有UPMS字典读取方式：`GET /v1/dict/type/code/{typeCode}/data`。

- `ai_model_provider`：现在表示服务商，初始值 `aliyun`、`openai`、`deepseek`。
- `ai_model_type`：`chat`、`embedding`、`rerank`、`image`、`speech_to_text`、`text_to_speech`。图片生成与聊天模型图片理解不是同一概念。
- `ai_model_protocol`：接入协议，`dashscope`、`ollama`、`openai_compatible`。原来读取 `ai_model_provider` 作为协议选项的代码必须删除并替换。
- `ai_model_auth_type`：`none`、`api_key`。

字典展示标签，向后端提交稳定的字典值。服务商与协议独立选择，不根据服务商硬编码唯一协议；增加字典项不代表后端已支持新协议。

## 连接器接口与表单

- 分页：`GET /v1/model-connections?current=1&size=10&modelId=...&dataStatus=1`，后两项可选。
- 详情：`GET /v1/model-connections/{id}`。
- 新建：`POST /v1/model-connections`。
- 更新：`PUT /v1/model-connections/{id}`，必须带当前 `version`。
- 删除：`DELETE /v1/model-connections/{id}`，仍被知识库使用时返回业务错误。

新增请求字段只有：

```json
{
  "modelId": "所选模型ID",
  "protocol": "openai_compatible",
  "baseUrl": "租户在服务商处获取的实际HTTP或HTTPS地址",
  "authType": "api_key",
  "apiKey": "用户输入的密钥"
}
```

更新使用相同字段并加 `version`；`modelId`不可切换，换模型应重新创建连接器。新建 `api_key` 认证必须填写密钥；编辑密钥留空表示保留，切换 `none` 表示清空存储凭证。不回显明文或掩码密钥，不把掩码当真实值提交。`baseUrl`不得含URL用户信息、查询参数及片段。

列表和详情字段：`id`、`dataStatus`、`version`、`createTime`、`updateTime`、`modelId`、`protocol`、`baseUrl`、`authType`、`credentialConfigured`、`modelName`、`modelType`、`providerCode`、`logoPath`。直接展示后端关联返回的模型信息，不逐条请求模型详情拼列表。一个模型可建立多条连接，列表用协议、地址辅助区分，不假设模型ID等于连接器ID。

交互流程：选择模型 → 用户选择协议 → 展示密钥获取入口、接入指南和地址说明 → 填写实际地址及认证信息 → 保存。链接为空时隐藏入口，Logo为空时使用项目统一占位图标。不要把某个租户的实例地址当成公共默认值。

所有接口沿用框架统一响应，成功code为2000，分页位于data中；Long业务ID按项目约定作为字符串处理，时间字段沿用现有时区显示。模型目录要求 `ai:model:read`，从目录创建连接还需 `ai:model-connection:write`；连接列表与详情要求read，写操作要求write。

本次只提供配置保存能力，没有连通性测试、远程模型发现或实际调用接口，不要增加虚假的“已验证”状态及测试按钮。
