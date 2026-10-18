# 给前端 AI 的实施提示词

请在现有前端工程中实现 AI 基础管理页面。先阅读前端工程自身的 AGENTS.md、路由、HTTP客户端、权限指令、字典组件及页面风格，再按以下真实后端合同开发。不要创建独立演示站点，不使用模拟数据代替已提供的接口。

本版范围是模型连接配置、模型草稿、知识库配置、目录、原始文件上传/替换/下载/删除。不提供聊天、模型测试、文档解析、文本分片预览、Embedding、重排、全文检索、向量检索或混合检索执行接口；不要创建对应假按钮、假成功状态或调用猜测的URL。

## 服务与基础协议

AI服务默认端口8300，UPMS默认端口8100。沿用前端现有代理/环境配置分别定位这两个服务，下面路径都是后端真实路径；代理前缀由前端工程决定，不写死开发者的服务器地址。

所有AI业务API携带 `Authorization: Bearer <UPMS访问令牌>`，租户上下文沿用现有工程机制。不新增AI登录，不自行伪造用户、租户、组织或角色范围。文件对象不能匿名访问，`isPublic`只表示当前租户内公开读取。

响应沿用 `ResultModel<T>`：`{code,devMessage,message,data,url}`，**业务成功码是2000**。HTTP 200不一定业务成功；同时处理HTTP 400参数错误、401登录失效、403访问被拒绝和非2000业务码。不要展示堆栈或原始内部错误。

所有业务ID及父ID均按十进制字符串管理，不转换为JavaScript Number；底座会将Long类型输出为字符串，因此fileSize、partSizeBytes、分页total等也可能是十进制字符串；需要数值计算时先校验安全范围再转换，分片号、排序值等Integer字段为数值。version原样回传，不自行递增。更新时回传读取到的 `version`，遇到版本冲突刷新后重试，不自行覆盖。

分页接口统一参数 `current=1`、`size=10`（1～200），返回 `data.records/current/size/total/pages`。目录接口返回平铺节点数组，不是分页，不是已经嵌套好的树。

## 页面和权限资源

接口使用框架 `@Permission`，注解直接声明资源码，不再通过AiPermission常量类中转。

| 功能 | 后端资源码 | 读取authority | 写入authority |
| --- | --- | --- | --- |
| 模型连接 | `ai:model-connection` | `ai:model-connection:read` | `ai:model-connection:write` |
| 模型配置 | `ai:model-config` | `ai:model-config:read` | `ai:model-config:write` |
| 知识库 | `ai:knowledge-base` | `ai:knowledge-base:read` | `ai:knowledge-base:write` |
| 目录 | `ai:knowledge-folder` | `ai:knowledge-folder:read` | `ai:knowledge-folder:write` |
| 文件及分片上传 | `ai:knowledge-document` | `ai:knowledge-document:read` | `ai:knowledge-document:write` |

GET对应read，POST/PUT/DELETE对应write。没有read权限不能使用对应功能；有接口权限后，具体记录的租户和数据范围继续由框架判断。公开资料不绕过功能权限，不允许匿名访问。

目录、文件的操作按钮须同时满足write权限和后端 `writable=true`；公开标记不扩大管理权限。分片上传复用文件资源的write权限。AI菜单permissionCode填写上述资源码，不追加read/write后缀。

## 模型连接

根路径 `/v1/model-connections`：

| 方法与路径 | 请求/返回 |
| --- | --- |
| GET 根路径 | 分页列表，参数current、size |
| GET `/{id}` | 详情 |
| POST 根路径 | 新增，data为连接ID |
| PUT `/{id}` | 更新，data=true |
| DELETE `/{id}` | 删除，data=true；仍有模型时拒绝 |

保存字段：`connectionName`必填≤100，`provider`必填，`baseUrl`必填≤1000，`authType`必填，`apiKey`可选≤4096，`remark`可选≤500；PUT另需version。地址只允许HTTP(S)且不能含用户名密码、查询参数或URL片段。

provider取字典 `ai_model_provider`，是接入协议，不是厂商品牌。authType取 `ai_model_auth_type`。

API Key只写不读：详情只返回 `credentialConfigured`。编辑时密钥输入框保持空白，空白表示保留原凭证；不要把星号、掩码、旧密钥或credentialConfigured填进apiKey。切换为 `none` 清除后端凭证。API Key首次保存需要后端配置AES主密钥；配置未就绪时展示业务错误，不将密钥存入localStorage、URL或日志。

响应字段：id、version、createTime、updateTime、connectionName、provider、baseUrl、authType、credentialConfigured、remark。

## 模型配置

根路径 `/v1/model-configs`，提供同样的GET分页、GET `/{id}`、POST、PUT `/{id}`、DELETE `/{id}`。分页可附 `modelType`。

保存字段：`connectionId`必填，`modelName`必填≤100，`modelType`必填，`modelCode`必填≤200，`remark`可选≤500；PUT另需version。连接从当前租户的连接分页加载，必须支持翻页，不假设只有200条。

modelType取字典 `ai_model_type`。embeddingDimensions由后端实测后填写，当前为空，仅作为只读信息展示，不能添加维度输入框或在保存请求中提交。当前未实测，**不得标为已验证或已接入成功**。

响应除保存字段外有id、version、createTime、updateTime、只读embeddingDimensions、`verified=false`。本版仅维护六种能力的草稿，不意味着六种能力的调用适配器已经启用。不要展示“测试连接”“测试向量”等未实现按钮。能力扩展JSON、调用参数JSON和知识库模型绑定不在当前保存DTO中，不要自行提交。

## 知识库

根路径 `/v1/knowledge-base`，提供GET分页、GET `/{id}`、POST、PUT `/{id}`、DELETE `/{id}`。分页可附keyword（名称关键字）。非空知识库不能删除。

保存字段：

| 字段 | 约束/默认值 |
| --- | --- |
| knowledgeName | 必填，≤100 |
| description | 可空，≤1000 |
| retrievalMode | 必填，keyword/vector/hybrid，字典 `ai_knowledge_retrieval_mode` |
| chunkMaxTokens | 1～32768，省略默认512 |
| chunkOverlapTokens | 0～32767且小于chunkMaxTokens，省略默认64 |
| keywordTopK / vectorTopK | 各1～200，省略默认20 |
| retrievalTopK | 1～200，省略默认5；keyword不超过keywordTopK，vector不超过vectorTopK，hybrid不超过两者之和 |
| similarityThreshold | 可空，0～1，仅保存向量阈值草稿 |
| version | PUT必填 |

这些是未来解析/检索策略的草稿保存，不启动工作流。初始页面选择keyword。模型绑定字段只读展示为空，本版没有绑定和验证模型API，不虚构下拉保存能力。

## 目录

根路径 `/v1/knowledge-base/{baseId}/folders`：GET读取平铺树节点，POST新增，PUT `/{id}`更新，DELETE `/{id}`删除。

保存字段：`parentId`可空（根目录），`folderName`必填≤100，`sortOrder`必填非负，`isPublic`可选（新增默认false），PUT需version。前端不提交orgId、tenantId、createBy。后端使用当前用户主组织，未配置主组织时按业务错误提示。

节点响应：id、version、dataStatus、createTime、updateTime、parentId、folderName、sortOrder、isPublic、navigationOnly、writable。节点同级按sortOrder、ID排序；ID比较避免Number精度损失，可使用BigInt比较。

`navigationOnly=true`表示该祖先仅用于到达可读子项，隐藏写操作，不推导其余文件可读。构建树时保留这些节点。公开目录不自动公开文件，公开文件也不会被私有目录阻断；后端已经过滤可见节点，前端不要再依据部门自行过滤。

同级目录不能重名，不能移动到自身/后代目录，非空目录不可删除；即使页面看起来为空，后端也可能因为不可见子项拒绝删除。只允许同一知识库内移动。

## 文件列表和管理

根路径 `/v1/knowledge-base/{baseId}/documents`：

| 方法与路径 | 用途 |
| --- | --- |
| GET 根路径 | 分页，支持folderId、uncategorized、keyword、current、size、dataStatus |
| GET `/{id}` | 文件详情 |
| GET `/{id}/download-url` | data为 `{url,expiresInSeconds:300}`，签发前重新校验权限 |
| PUT `/{id}` | 修改元数据 |
| DELETE `/{id}` | 删除并登记异步原件清理，data=true |

GET省略folderId且uncategorized=false表示全部，uncategorized=true表示未分类，folderId与uncategorized=true不能同时传。“全部”“未分类”是虚拟筛选入口，不插入目录记录。

PUT字段：`documentName`必填≤255、`folderId`可空（移到未分类）、`isPublic`必填、`version`必填。公开开关的说明为“租户内公开”，不是互联网公开。移动不改变文件的组织或公开状态。

文件响应：id、version、dataStatus、createTime、updateTime、knowledgeBaseId、folderId、orgId、createBy、documentName、originalFilename、contentType、fileSize、fileSha256、isPublic、processingStatus、processingVersion、writable。不返回MinIO桶、对象键、访问密钥。

本版新文件状态为 `uploaded`，显示“已上传”。只能预览原件：浏览器支持的PDF/UTF8文本可在取得授权URL后获取Blob展示，其余格式提供下载。不得把原件预览说成分片预览；不要将用户文本作为HTML执行。URL过期或失效重新向后端申请，不长期存储签名URL。

## 必须封装 MultipartFileUpload 组件

组件负责策略读取、校验、切片、并发、重试、进度、ETag、合并确认和取消，不把传输逻辑重复写在各业务页面。通过props注入businessCode、两个参数键、knowledgeBaseId、folderId、apiAdapter、disabled；替换时增加replaceDocumentId、replaceVersion。

AI场景businessCode为 `ai-agent`。组件成功事件只在后端完成确认返回documentId之后触发。所有文件都走分片，小文件只有一个最终分片。

根路径 `/v1/knowledge-base/{baseId}/uploads`：

1. POST初始化：`{originalFilename,fileSize,partSizeBytes,concurrency,maxRetries,urlExpirySeconds,sessionExpireSeconds,folderId?,replaceDocumentId?,replaceVersion?}`。五个传输参数为必填，来自客户端已读取并校验的系统参数；不提交allowedExtensions或maxFileSizeBytes。新文件不要指定替换字段。data返回 `{taskId,partSizeBytes,partCount,concurrency,maxRetries,urlExpirySeconds,expiresAt}`。
2. 按任务快照调用File.slice，不能使用前端缓存策略覆盖partSizeBytes或partCount。
3. POST `/{taskId}/parts/{partNumber}/url`，空请求体，data为 `{url,partNumber}`。partNumber从1开始。将分片内容直接PUT到返回url；此PUT不携带SaaS Authorization，不修改URL、Host或签名查询参数。
4. 读取每个PUT响应ETag，作为不透明字符串保存。组件并发是整个队列的上限，不是每个文件各自开启无限任务。片失败最多额外重试maxRetries次；URL过期时重新申请对应片URL。
5. 所有片成功后POST `/{taskId}/complete`：`{parts:[{partNumber:1,etag:"..."},...]}`，序号升序且完整。data为 `{documentId:"雪花ID"}`。后端核验存储实际片数、顺序、ETag及总字节数并计算SHA256；不执行格式、编码或业务大小上限校验。合并响应丢失可以重试同一complete，不能直接创建第二个任务；已完成返回同一ID。
6. DELETE `/{taskId}`取消，data=true。取消不删除已完成文件。组件卸载或取消先中断进行中的请求，再调用取消API；浏览器未能取消的任务由后端按expiresAt清理。

“替换文件”是显式操作：传原documentId、当前version和原folderId，后端保留文件ID、显示名称、目录、组织归属和公开状态。相同文件名普通上传仍创建新文件，不自动覆盖。替换失败保留旧文件；成功后旧处理轮次与旧检索数据失效，本版只回到uploaded，不执行模型调用。

本版不承诺页面刷新后的断点续传。不要把S3 ETag当成文件SHA256，不让用户填写bucket、objectKey或真实MinIO uploadId。

浏览器联调前确认MinIO允许实际前端Origin、PUT预检以及暴露ETag响应头；签名URL中的S3地址必须前后端均可访问。CORS或网络失败按真实错误反馈，不改为把文件偷偷传入其他云存储。

## 字典：当前六种类型，以及本次新增项

通过UPMS已有接口读取：`GET /v1/dict/type/code/{typeCode}/data`，使用返回项的dictLabel展示、dictValue提交，按sort排列。不把dictCode（如CHAT）作为接口值。

| typeCode | dictValue |
| --- | --- |
| ai_model_type | chat、embedding、rerank、image、speech_to_text、text_to_speech |
| ai_model_provider | dashscope、ollama、openai_compatible |
| ai_model_auth_type | none、api_key |
| ai_knowledge_document_status | **uploaded**、pending、parsing、preview、embedding、ready、failed |
| ai_knowledge_chunk_embedding_status | not_required、pending、ready、failed |
| ai_knowledge_retrieval_mode | keyword、vector、hybrid |

六种类型已在AI模块的data.sql维护。本次没有新增第七种字典类型，仅为文档状态增加 `dictCode=UPLOADED, dictValue=uploaded, dictLabel=已上传`。本版没有分片页面，分片状态字典留待后续使用；字典中存在解析/向量化状态，不表示对应接口已实现。isPublic直接使用布尔开关，不新增重复字典。

## 系统参数：不能复用通用上传参数

从UPMS获取：

```http
GET /v1/config/values?parameterKeys=ai-agent-file-type&parameterKeys=ai-agent-file-upload
```

`ResultModel.data`是 `Map<string,string>`，每一项是JSON字符串，需要分别JSON.parse。解析失败、缺失或参数不可用时停止新建上传，不退回其他业务的参数。

`ai-agent-file-type`：

```json
{"allowedExtensions":[".txt",".md",".pdf",".docx",".xlsx",".csv"]}
```

扩展名小写且带点，生成accept，并在选择文件及初始化前校验最后一个扩展名（不区分大小写）。文件类型和业务大小上限完全由客户端限制，不能仅依赖input的accept属性；AI后端不再限制六类后缀、文本编码或检测文件容器。新增扩展名只调整参数即可接收原件，不表示已支持解析或预览。

`ai-agent-file-upload`：

```json
{"maxFileSizeBytes":104857600,"partSizeBytes":8388608,"concurrency":3,"maxRetries":3,"urlExpirySeconds":900,"sessionExpireSeconds":86400}
```

单位分别为字节、字节、数量、次数、秒、秒。初始为单文件100MiB、每片8MiB、队列并发3、额外重试3次、签名900秒、任务86400秒。参数控制新任务，已有任务以初始化响应快照为准。客户端必须自行校验参数有效性、文件扩展名及0 < file.size <= maxFileSizeBytes，校验不通过不调用初始化API。AI后端不读取系统参数，不依赖UPMS参数服务；初始化时提交上述五个传输参数，后端只保留S3协议边界、权限和传输完整性检查。详细步骤和验收用例见[客户端上传校验实施说明](frontend-upload-client-validation.md)。

## 必须返回的菜单交付物

完成代码和前端验证后，**必须生成 `docs/ai-menu-manifest.json`，并在最终回复中贴出对应JSON或提供文件内容**，用户会将它交给后端AI初始化菜单。本次不要调用菜单创建接口，不直接执行初始化SQL。

菜单只支持现有 `DIRECTORY`、`MENU`，不要新增BUTTON类型。目录permissionCode为null；MENU填写上述资源码，按钮authority按页面的write权限判断。

请返回真实存在的路由和组件。五个功能需要各自对应可授权的MENU节点；目录、文档可作为知识库子菜单并复用组件，但其路径必须已在前端路由注册。不要为了填清单虚构尚不存在的页面。

交付JSON格式：

```json
{
  "menus": [
    {
      "key": "ai-root",
      "parentKey": null,
      "menuName": "AI管理",
      "menuType": "DIRECTORY",
      "menuPath": "填写实际路径",
      "menuRouter": "填写工程实际路由或组件标识",
      "permissionCode": null,
      "menuIcon": "填写实际图标",
      "sort": 100
    },
    {
      "key": "ai-model-connection",
      "parentKey": "ai-root",
      "menuName": "模型连接",
      "menuType": "MENU",
      "menuPath": "填写实际路径",
      "menuRouter": "填写实际路由或组件标识",
      "permissionCode": "ai:model-connection",
      "menuIcon": "填写实际图标",
      "sort": 10
    }
  ],
  "buttons": [
    {"menuKey":"ai-model-connection","label":"新增连接","authority":"ai:model-connection:write"}
  ],
  "routes": [
    {"menuKey":"ai-model-connection","path":"填写实际路径","name":"填写实际路由名","component":"填写实际组件文件路径"}
  ],
  "apiServiceConfig": {"ai":"填写实际代理配置名","upms":"填写实际代理配置名"},
  "validation": {"build":"填写实际执行结果","typecheck":"填写实际执行结果","integration":"填写实测情况和未完成事项"}
}
```

补齐其余四个功能，不要原样保留示例占位文本。key/parentKey只用于表达层级，后端会分配雪花ID，不自行生成数据库ID。menuName不超过10个字符，menuPath/menuRouter不超过100，menuIcon不超过200。说明前端路由字段与现有菜单动态路由机制如何对应。

最终同时返回：实现页面和组件清单、实际验证命令、菜单JSON、所有尚未完成的联调事项。没有运行后端或浏览器验证就明确说明，不将静态检查当成真实联调成功。

## 本轮增量：启停状态筛选与展示

请给以下五个管理列表统一增加“状态”下拉筛选：

| 页面 | GET接口 | 可选查询参数 |
| --- | --- | --- |
| 模型连接 | `/v1/model-connections` | `dataStatus` |
| 模型配置 | `/v1/model-configs` | `dataStatus`，可与`modelType`组合 |
| 知识库 | `/v1/knowledge-base` | `dataStatus`，可与`keyword`组合 |
| 目录树 | `/v1/knowledge-base/{baseId}/folders` | `dataStatus` |
| 文档列表 | `/v1/knowledge-base/{baseId}/documents` | `dataStatus`，可与目录、未分类、名称筛选组合 |

- 默认选择“全部”，不发送`dataStatus`参数；选择“启用”发送数值`1`，选择“禁用”发送数值`0`。清空或重置恢复全部，不发送`-1`、`all`或字符串`null`。查询参数的数值范围由后端校验。
- 复用现有系统字典`common_status`的启用/禁用选项，不新增AI字典或系统参数。“全部”是前端虚拟选项，不写入字典。字典值若为字符串，组装请求时显式转数值；不能用真假判断丢掉合法的`0`。
- 模型连接、模型配置、知识库、目录和文档VO均新增`dataStatus`，列表与详情展示状态标签。它与`processingStatus`（uploaded等处理进度）及`isPublic`（公开范围）是不同字段，不能混用。
- 使用服务端筛选，不能只过滤当前页记录。切换分页列表筛选时将`current`重置为`1`，保留其它搜索条件并使用服务端`total`；清空后重新请求全部状态。
- 目录筛选作用于目录自身状态，后端仍补齐必要祖先，即祖先可能与筛选值不一致。必须保留完整响应构建目录树，不按状态二次裁剪祖先。目录状态不替代文档状态：切换目录时文档列表继续使用自己的状态筛选。
- 管理详情允许读取已授权的禁用记录，不因禁用隐藏详情或元数据维护入口。原件下载/预览由后端继续校验知识库和文件是否启用；收到停用提示时正常展示业务错误，不靠前端传`dataStatus=1`绕过。
- 本轮增加筛选和状态展示，不新增启停写接口。继续按现有实际接口执行，不把`dataStatus`擅自加入保存DTO。
- 本轮无新增菜单资源或权限码。完成后仍按本文既有要求返回菜单清单，并说明五个页面的筛选联动、禁用值`0`的请求和目录祖先保留的验证结果。
