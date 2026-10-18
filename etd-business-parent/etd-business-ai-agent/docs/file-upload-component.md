# AI 文件分片上传与前端组件约定

本版只实现页面管理和原始文件上传、下载、删除，不解析文档、不进行文本分片、不调用模型、不生成向量。本文的“分片”指文件传输分片，与后续知识库的文本分片不同。

当前已实现AI上传接口、持久化任务、传输完整性检查和后台清理；模块系统参数由客户端读取并校验，前端组件尚未在本仓库实现。客户端增量实施要求见[客户端上传校验实施说明](frontend-upload-client-validation.md)。实际请求响应及菜单交付格式以 [前端AI实施提示词](frontend-ai-prompt.md) 为准。

## 模块独立的系统参数

参数由 AI 工程 `src/main/resources/sql/postgresql/init.sql` 初始化到已有 `sys_config` 所在数据库，沿用系统参数页面维护。参数是当前平台的全局模块级策略，不是租户可以任意覆盖的模型或凭证配置。

| 参数键 | 类型 | 用途 |
| --- | --- | --- |
| `ai-agent-file-type` | `json` | AI 文件扩展名白名单 |
| `ai-agent-file-upload` | `json` | AI 文件大小与分片传输策略 |

其他业务模块采用各自的 `<业务模块>-file-type`、`<业务模块>-file-upload` 参数，组件通过业务配置注入参数键，不复用 `system.resource.limit.upload`，也不把 AI 参数作为其他业务的默认值。

`ai-agent-file-type` 初始值：

```json
{"allowedExtensions":[".txt",".md",".pdf",".docx",".xlsx",".csv"]}
```

所有扩展名使用带点小写字符串且去重；不允许 `*`、`.*`、路径、空项或目录穿越。判断文件最后一个扩展名，比较前转为小写；例如 `report.PDF` 按 `.pdf` 判断。允许接收这些文件只代表可以保存原件，不代表解析、OCR或分片能力已经实现。

`ai-agent-file-upload` 初始值：

```json
{
  "maxFileSizeBytes": 104857600,
  "partSizeBytes": 8388608,
  "concurrency": 3,
  "maxRetries": 3,
  "urlExpirySeconds": 900,
  "sessionExpireSeconds": 86400
}
```

| 字段 | 初始值 | 单位及约束 |
| --- | --- | --- |
| `maxFileSizeBytes` | 104857600 | 单文件100MiB；正整数，不接受空文件 |
| `partSizeBytes` | 8388608 | 每片8MiB，由客户端读取参数后随初始化请求提交；除最后一片外遵守分片最小尺寸要求 |
| `concurrency` | 3 | 组件上传队列的并发分片总数，不是每个文件各自无限开启任务 |
| `maxRetries` | 3 | 某片首次失败后最多额外重试3次；已成功分片不重复上传 |
| `urlExpirySeconds` | 900 | 每个分片签名有效秒数，由后端签发；过期后重新申请，不直接修改URL |
| `sessionExpireSeconds` | 86400 | 上传任务最长有效秒数，后端清理未完成任务的依据 |

客户端负责参数类型、范围及组合校验：扩展名白名单非空且为带点小写字符串，单文件上限为正整数；普通分片5MiB～5GiB、实际总片数不超过10000，并发为正整数，额外重试次数非负，签名有效期1～604800秒，任务有效期为正整数秒。整数字段必须是安全整数，并发、重试、签名及任务有效期不能超过Java Integer上限。`maxFileSizeBytes`及允许扩展名不发送给AI后端。

参数未配置、被停用或值非法时，由组件停止新建上传并展示“上传配置暂不可用”；文件类型或大小不符合要求时，在客户端提示原因且不调用上传初始化接口。后端不读取、校验这些系统参数，也不因UPMS不可用而阻断接口。客户端不得悄悄退回通用参数或硬编码白名单。策略变更对新任务生效，已创建任务使用保存的传输参数快照。

## 前端从哪里读取

使用 UPMS 已有的参数批量读取接口，路径相对于 UPMS 服务或前端网关的对应代理前缀：

```http
GET /v1/config/values?parameterKeys=ai-agent-file-type&parameterKeys=ai-agent-file-upload
```

当前接口返回 `ResultModel<Map<String,String>>`，即使参数的 `value_type=json`，`data` 中每个参数值仍是 **JSON字符串**，不是已解析的对象。取出下列两项并分别执行一次 `JSON.parse`：

```ts
// 请求和错误处理沿用项目HTTP客户端；以下仅说明响应数据的解析层级。
const typePolicy = JSON.parse(response.data['ai-agent-file-type']);
const uploadPolicy = JSON.parse(response.data['ai-agent-file-upload']);
const accept = typePolicy.allowedExtensions.join(',');
```

这里的 `response.data` 指已解包到 `ResultModel.data` 的映射；如果HTTP库没有解包，先按既有项目约定取业务 `data`，避免把HTTP响应层和业务响应层混用。前端请求只传这两个公开策略键，不拉取全部系统参数。系统参数管理页可以修改这两条JSON参数，内置标志不等于禁止修改参数值，沿用现有UPMS行为。

## 必须封装为复用组件

建议组件名 `MultipartFileUpload`，AI知识库页面通过组件接入，不在页面内重复实现切片、并发、进度、签名及合并逻辑。组件只负责传输和展示，不承担模型调用、解析或文本分片。

| 组件输入 | 作用 |
| --- | --- |
| `businessCode` | AI场景固定为 `ai-agent`，由页面配置，不由用户填写 |
| `fileTypeParameterKey` | AI使用 `ai-agent-file-type`；供其他业务注入自己的参数键 |
| `uploadPolicyParameterKey` | AI使用 `ai-agent-file-upload` |
| `knowledgeBaseId` | 当前操作的知识库ID，雪花ID按字符串传输 |
| `folderId` | 可空，目标目录ID字符串，空表示未分类；后端校验同租户同知识库及写入权限 |
| `apiAdapter` | 注入初始化、申请签名、完成及取消等业务接口调用；组件不写死业务接口前缀 |
| `disabled` | 缺少文件write权限、页面只读或目标目录/文件的 writable=false 时禁用 |

组件提供上传进度、成功、失败、取消事件；成功事件只在后端确认合并、校验并登记文件后触发，返回后端文件/文档ID。页面不能仅根据所有PUT完成就显示“上传成功”。文件列表优先展示当前字节进度，依次区分上传、合并、校验、已上传、失败和取消；本版不展示解析或向量化成功。

## 分片直传流程及API

文件内容由浏览器通过短期预签名URL直接PUT到本地MinIO，应用服务负责鉴权、签名和完成确认；前端不得接触MinIO AccessKey或SecretKey。所有文件均走分片流程，小于单片大小的文件只发送一个最终分片。

以下接口已由UploadController实现，路径变量knowledgeBaseId在代码中命名为baseId，不影响实际路径：

| 接口 | 意图 |
| --- | --- |
| `POST /v1/knowledge-base/{knowledgeBaseId}/uploads` | 初始化上传任务，校验当前租户、知识库、文件名和声明大小 |
| `POST /v1/knowledge-base/{knowledgeBaseId}/uploads/{taskId}/parts/{partNumber}/url` | 为指定任务分片申请签名，支持失败后重新申请 |
| `POST /v1/knowledge-base/{knowledgeBaseId}/uploads/{taskId}/complete` | 校验分片、完成合并、核验对象并登记文件，重复确认不能创建重复文档 |
| `DELETE /v1/knowledge-base/{knowledgeBaseId}/uploads/{taskId}` | 终止上传并清理未完成分片，不等于删除已经上传完成的文档 |

`taskId` 为后端生成的雪花ID字符串；真正的MinIO `uploadId`、bucket、objectKey等只由后端任务上下文维护。每一步校验任务所属租户、用户授权、知识库及有效期，不允许前端修改存储桶、对象键或传入任意MinIO上传任务ID。

初始化时可以指定 `folderId`，目录只负责分类，公开目录不使上传文件自动公开。上传文件的 `is_public` 默认false，由有管理权限的业务操作显式设置；`org_id` 由后端可信上下文填充或经授权校验后指定。合并登记时重新校验目标目录有效且可写，不能仅复用初始化时的旧权限。替换已有文件保留文件ID、目录、组织归属和公开状态，不根据新文件名或上传人的组织静默覆盖。

初始化请求必须增加 `partSizeBytes`、`concurrency`、`maxRetries`、`urlExpirySeconds`、`sessionExpireSeconds`，值来自客户端已校验的系统参数。初始化返回taskId、保存的 `partSizeBytes`、服务端计算的 `partCount`、expiresAt及concurrency/maxRetries/urlExpirySeconds传输参数快照。组件按此响应执行 `File.slice`，不能以页面旧缓存的分片大小覆盖任务快照。分片编号从1开始，最大10000；最后一片允许小于普通分片大小。

组件上传每片后保存响应的 `ETag`，按 `partNumber` 升序提交全部分片信息。ETag应作为不透明值保存，按后端协议统一处理引号，不能把分片或合并对象的ETag当作完整文件SHA-256。签名过期仅重新签发对应分片，不创建第二个任务；合并响应丢失后通过幂等完成确认恢复，不直接重新上传整个文件。

组件关闭、主动取消时中断网络请求并调用后端终止接口；页面刷新后第一版不承诺断点续传。后端还需清理超时任务及未完成分片，不能仅依赖浏览器执行取消。

## 后端与MinIO的实施要求

- 复用框架 `FileUpload.initMultipart`、`generatePartUrl`、`completeMultipart`。业务层通过MinIO SDK补充分片清单、取消、对象核验及清理，框架公共API保持不变；业务Controller只接收受控DTO，不直接暴露存储starter请求模型。
- 文件扩展名白名单和业务大小上限仅由客户端校验，AI后端不访问UPMS参数接口。后端只校验S3传输协议及实际分片清单的片数、尺寸、ETag和合并后总字节数；不再检测PDF文件头、Office容器或文本编码。MIME按文件名推断，未知类型保存为application/octet-stream，不代表已经校验格式。
- 后端生成包含租户和知识库边界的对象键。声明大小不能约束预签名PUT的实际字节数，必须限制签名申请并校验最终对象；不合规对象不登记为已上传并执行清理。
- SHA-256如需落入现有文档表，由后端流式计算并保存；此步骤不等于文档解析。文件完成登记前，不应伪造摘要、解析状态或填入无意义分片配置来满足未来阶段的非空约束。
- MinIO浏览器直传需配置允许的前端Origin、PUT预检及暴露 `ETag` 响应头。保留SaaS业务鉴权只用于应用API，对预签名URL请求不附加应用Authorization头，不改写已签名的URL、Host或路径。签名使用浏览器实际可访问的S3 API地址，不能使用Console端口或浏览器不可达的容器地址。
- 上传传输任务和AI文档解析任务是不同生命周期。新文件登记为uploaded，原件替换清空旧解析租约及结果快照；不实现解析、文本分片、向量化或检索接口。

参考：[MinIO CORS与ETag暴露](https://docs.min.io/aistor/administration/cors-configuration/)、[分片上传限制](https://docs.aws.amazon.com/AmazonS3/latest/userguide/qfacts.html)。这里引用的是MinIO兼容的S3传输协议，不要求使用AWS云存储或云账户。

## 本次新增的持久化支持

ai_file_upload保存初始化状态、真实MinIO任务ID、策略快照、原用户、目标目录、替换文件版本及完成结果。初始化前先独立提交任务，即使进程在MinIO创建后中断，也能按后端唯一对象键查找并清理。完成确认持有知识库、会话及旧文件行锁；存储已合并但数据库未提交时，重试核验唯一对象后恢复登记。

ai_storage_cleanup在文件替换、删除、取消及过期处理的同一数据库事务内登记清理意图，后台按批次FOR UPDATE SKIP LOCKED处理。失败保留任务并延迟重试；当前仍被未删除文件引用的对象不会进入删除批次。清理成功后物理移除队列记录，不把网络调用伪装成数据库原子事务。
