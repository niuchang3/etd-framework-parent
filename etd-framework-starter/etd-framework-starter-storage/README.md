# etd-framework-starter-storage 存储 Starter 使用指南

`etd-framework-starter-storage` 是 ETD 框架中统一屏蔽对象存储服务差异的 **开箱即用存储模块**。支持 **MinIO**（私有化部署）与 **阿里云 OSS**（公有云）等多种厂商，提供 Web 端直传、大文件分片直传、服务端物理上传/下载以及带时效性 GET 外链动态生成全套能力。

---

## 一、 快速开始与 YAML 配置

在业务应用（如 `etd-business-upms`）的 `pom.xml` 中引入依赖：

```xml
<dependency>
    <groupId>org.etd.framework</groupId>
    <artifactId>etd-framework-starter-storage</artifactId>
</dependency>
```

在 `application.yml` 中配置存储参数：

```yaml
storage:
  # MinIO 存储配置
  minio:
    enabled: true
    endpoint: http://minio.etd.org:9000
    access-key: etd_admin
    secret-key: etd_password_2026
    expiry: 604800 # 默认预签名 URL 有效时长（单位：秒，7天）

  # 阿里云 OSS 存储配置
  alibaba:
    enabled: false
    endpoint: https://oss-cn-hangzhou.aliyuncs.com
    access-key: LTAI5tXXXXXX
    secret-key: XXXXXXsecretkey
    expiry: 604800
```

---

## 二、 后端 Java 接口说明与示例 (`FileUpload` & `FileDownload`)

为了遵循单一职责与接口隔离原则，存储模块将 **上传** 与 **下载/外链生成** 拆分为两个独立的策略接口：

- **`FileUpload` 接口**：专注单文件直传、分片直传与服务端物理上传；
- **`FileDownload` 接口**：专注生成带时效性的在线预览/下载外链，以及服务端物理流/字节下载。

### 1. 依赖注入与调度方式

```java
@RestController
@RequestMapping("/file")
public class FileController {

    // 方式 A：按职责注入上传与下载接口 Bean
    @Autowired
    private FileUpload fileUpload;     // 注入上传客户端

    @Autowired
    private FileDownload fileDownload; // 注入下载/预览客户端

    // 方式 B：通过 StorageContext 动态获取上传或下载客户端
    FileUpload uploadClient = StorageContext.getUploadClient(StorageContext.ClientType.MinIo);
    FileDownload downloadClient = StorageContext.getDownloadClient(StorageContext.ClientType.MinIo);
}
```

---

### 2. 上传能力一览 (`FileUpload`)

#### ① 单文件预签名直传 (`generateUploadUrl`)
```java
UploadUrlReqModel reqModel = new UploadUrlReqModel();
reqModel.setBucketName("avatar");
reqModel.setDirectory("2026/08");
reqModel.setOriginalFileName("photo.png");

UploadUrlResModel res = fileUpload.generateUploadUrl(reqModel);
```

#### ② 大文件分片直传 3 步 API
```java
InitMultipartResModel initRes = fileUpload.initMultipart(initReq);
GeneratePartUrlResModel partRes = fileUpload.generatePartUrl(partReq);
CompleteMultipartResModel compRes = fileUpload.completeMultipart(compReq);
```

#### ③ 服务端物理上传 (`upload`)
```java
// 支持 InputStream, byte[], MultipartFile
ServerUploadResModel res = fileUpload.upload(inputStreamReqModel);
```

---

### 3. 下载与外链生成能力一览 (`FileDownload`)

#### ① 预签名 GET 外链生成 (`generateObjectUrl`)
适用于 Web / App 端直接点开预览或触发浏览器下载（流量不走后端）：

```java
GenerateObjectUrlReqModel reqModel = new GenerateObjectUrlReqModel();
reqModel.setBucketName("contract");
reqModel.setFileName("2026/08/A1B2C3.pdf");
reqModel.setExpiry(3600); // 1 小时有效

GenerateObjectUrlResModel res = fileDownload.generateObjectUrl(reqModel);
// res.getFileUrl() -> "https://minio.domain.com/contract/2026/08/A1B2C3.pdf?X-Amz-Signature=..."
```

#### ② 服务端代理流式下载 (`downloadInputStream`)
适用于最高安全等级的敏感绝密文件下载，由 Spring Boot Controller 进行权限校验后管道流式输出给前端：

```java
@GetMapping("/download/secret")
public ResponseEntity<Resource> downloadSecretFile(@RequestParam String fileName) throws Exception {
    // 1. 进行权限校验（如 Spring Security 鉴权）
    
    // 2. 服务端直接获取云端 InputStream 输入流
    DownloadObjectReqModel reqModel = new DownloadObjectReqModel();
    reqModel.setBucketName("secret-docs");
    reqModel.setFileName(fileName);
    InputStream inputStream = fileDownload.downloadInputStream(reqModel);

    // 3. 以管道流 response 吐给前端
    InputStreamResource resource = new InputStreamResource(inputStream);
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
}
```

#### ③ 服务端字节下载 (`downloadBytes`)
适用于后端直接读取文件字节在内存中进行解析（如读取 Excel / PDF）：

```java
DownloadObjectReqModel reqModel = new DownloadObjectReqModel();
reqModel.setBucketName("data");
reqModel.setFileName("2026/08/excel_data.xlsx");

byte[] fileBytes = fileDownload.downloadBytes(reqModel);
// 直接在内存中解析字节...
```

---

## 三、 前端（Vue 3 / TypeScript / Axios）接入完整示例

### 场景一：单文件直传 (Small File Upload)

```typescript
import axios from 'axios';

export async function uploadSingleFile(file: File) {
  const res = await axios.post('/api/file/upload-url', {
    bucketName: 'avatar',
    directory: 'user',
    originalFileName: file.name,
    contentType: file.type
  });

  const { uploadUrl, fileName } = res.data;

  await axios.put(uploadUrl, file, {
    headers: {
      'Content-Type': file.type || 'application/octet-stream'
    }
  });

  console.log('直传成功，存盘 Key 为:', fileName);
  return fileName;
}
```

---

### 场景二：超大文件分片直传与合并 (Large File Chunk Upload)

```typescript
import axios from 'axios';

const CHUNK_SIZE = 5 * 1024 * 1024; // 默认分片大小: 5MB

export async function uploadLargeFileInChunks(file: File) {
  const bucketName = 'videos';
  const directory = 'course';

  const initRes = await axios.post('/api/file/multipart/init', {
    bucketName,
    directory,
    originalFileName: file.name,
    contentType: file.type
  });
  const { uploadId, fileName } = initRes.data;

  const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
  const partETags: Array<{ partNumber: number; eTag: string }> = [];

  for (let i = 0; i < totalChunks; i++) {
    const partNumber = i + 1;
    const start = i * CHUNK_SIZE;
    const end = Math.min(file.size, start + CHUNK_SIZE);
    const chunkBlob = file.slice(start, end);

    const partUrlRes = await axios.post('/api/file/multipart/part-url', {
      bucketName,
      fileName,
      uploadId,
      partNumber
    });
    const { partUrl } = partUrlRes.data;

    const putRes = await axios.put(partUrl, chunkBlob, {
      headers: {
        'Content-Type': file.type || 'application/octet-stream'
      }
    });

    const rawETag = putRes.headers['etag'] || putRes.headers['ETag'];
    const cleanETag = rawETag ? rawETag.replace(/"/g, '') : '';

    partETags.push({
      partNumber,
      eTag: cleanETag
    });
  }

  const completeRes = await axios.post('/api/file/multipart/complete', {
    bucketName,
    fileName,
    uploadId,
    parts: partETags
  });

  console.log('大文件合并成功! 在线访问外链:', completeRes.data.fileUrl);
  return completeRes.data;
}
```

---

## 四、 云端跨域 CORS 配置指南

由于前端是在浏览器直接向 MinIO / OSS 的域名发起 `PUT` 请求，必须在存储控制台为对应 Bucket 开启跨域配置：

- **Allowed Origins**: `*`
- **Allowed Methods**: `GET`, `PUT`, `POST`, `DELETE`, `HEAD`
- **Allowed Headers**: `*`
- **Exposed Headers**: `ETag` (必须暴露 ETag 供前端读取分片校验码)
