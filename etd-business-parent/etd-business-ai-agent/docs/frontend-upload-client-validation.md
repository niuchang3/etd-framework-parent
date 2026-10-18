# 客户端上传参数校验实施说明

本次调整将文件扩展名白名单、单文件大小上限及系统参数有效性校验交给客户端。AI服务不再读取UPMS系统参数，不再配置parameter-service-url，也不再检测PDF文件头、Office容器或文本编码。现有鉴权、任务归属、S3协议检查、分片ETag及实际字节数核对保持生效。

前端代码不在本仓库，以下要求交给前端开发落实到复用组件`MultipartFileUpload`，普通上传和替换上传必须复用同一校验入口。

## 读取并解析参数

从UPMS服务或网关对应代理读取，不向AI服务请求系统参数：

```http
GET /v1/config/values?parameterKeys=ai-agent-file-type&parameterKeys=ai-agent-file-upload
```

检查统一业务响应成功后，从`ResultModel.data`取出两个JSON字符串并分别`JSON.parse`，不要重复解析或误用HTTP响应层的data。读取失败、缺项、停用、JSON错误或字段不合法时，禁用新建上传并提示“上传配置暂不可用”；不使用硬编码策略或其他模块参数兜底。

| 参数字段 | 客户端校验 |
| --- | --- |
| allowedExtensions | 非空数组，元素为不重复的带点小写扩展名，例如.txt；拒绝空项、路径和通配符 |
| maxFileSizeBytes | 正的JavaScript安全整数，单位字节 |
| partSizeBytes | 安全整数，5242880～5368709120字节，这是S3普通分片协议范围 |
| concurrency | 正整数，不超过2147483647，作用于整个上传队列 |
| maxRetries | 非负整数，不超过2147483647，表示首次失败后的额外重试次数 |
| urlExpirySeconds | 整数，1～604800秒，这是S3预签名URL有效期范围 |
| sessionExpireSeconds | 正整数秒，不超过2147483647；上传期间以响应expiresAt为准 |

不要对数字字符串、null、布尔值使用隐式转换来接受非法参数。上述整数JSON字段必须原本就是number且通过`Number.isSafeInteger`。

## 每个文件必须执行的校验

选择文件时及调用初始化API之前，均执行以下检查：

1. 取文件名最后一个点及后缀并转小写；不存在扩展名或不在allowedExtensions内时提示“文件类型不支持”，不发起初始化请求。文件选择器accept只作展示，拖放、粘贴和程序添加文件也必须走校验。
2. 检查`file.size`为正的安全整数，且`file.size <= maxFileSizeBytes`；空文件和超限文件在客户端拒绝，并展示参数换算后的大小限制，不写死100MiB。
3. 根据本次partSizeBytes计算`Math.ceil(file.size / partSizeBytes)`，超过10000片时提示调整分片设置，不创建不可完成的任务。
4. 普通上传和替换上传执行同一套检查。批量文件逐个展示失败原因，不能因为其中一个不合规而偷偷跳过校验或换用其他上传接口。

客户端只控制接收原件的范围，不宣称这些格式已支持解析、OCR、向量化或浏览器预览。

## 初始化请求变更

初始化路径仍为：

```http
POST /v1/knowledge-base/{baseId}/uploads
```

原请求增加以下五个必填传输参数，值取自本次已经校验的系统参数：

```json
{
  "originalFilename": "report.pdf",
  "fileSize": 12345,
  "partSizeBytes": 8388608,
  "concurrency": 3,
  "maxRetries": 3,
  "urlExpirySeconds": 900,
  "sessionExpireSeconds": 86400
}
```

folderId、replaceDocumentId、replaceVersion沿用原有业务约定。allowedExtensions和maxFileSizeBytes仅在客户端使用，不发送给后端。不得自行增加租户、用户、桶、对象键或MinIO真实uploadId字段。

后端不为缺失的五个参数补默认值；缺失、类型不正确或违反S3协议时返回400。返回结构保持`taskId、partSizeBytes、partCount、concurrency、maxRetries、urlExpirySeconds、expiresAt`，其中partCount由服务端计算，客户端使用响应快照切片及调度。参数变更只影响后续新建任务，不改变已经创建的上传。

上传分片、收集ETag、完成确认、取消与重试接口保持原样。只有完成接口返回documentId后才触发上传成功事件；仅完成所有PUT不能直接显示成功。后端仅按文件名推断MIME并计算实际字节的SHA256，未知类型使用application/octet-stream。

## 前端交付验收

- 参数读取失败、停用、缺失及JSON错误：禁用上传且不发初始化请求。
- 允许后缀的大写形式可以通过；无后缀和白名单以外后缀被客户端拒绝。
- 等于大小上限允许，超过1字节拒绝；空文件拒绝，批量与替换上传结果一致。
- 初始化请求实际携带五个传输参数，不再依赖后端读取UPMS或返回默认策略。
- 修改系统参数后，新任务使用新值；进行中的任务继续使用自己的快照。
- 正常上传、替换、取消、重新申请签名及完成确认仍可用，权限控制不变。

请前端开发提供上述交互验证结果；本仓库的Java测试只验证后端行为，不能代替客户端验收。
