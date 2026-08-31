# etd-framework-starter-storage 存储 Starter 使用指南

`etd-framework-starter-storage` 是 ETD 框架中统一屏蔽对象存储服务差异的 **开箱即用存储模块**。支持 **MinIO**（私有化部署）与 **阿里云 OSS**（公有云）等多种厂商，提供 Web 端直传、大文件分片直传以及服务端流/字节/文件物理上传全套能力。

---

## 一、 快速开始与 YAML 配置

在业务应用（如 `etd-business-upms`）的 `pom.xml` 中引入依赖：

```xml
<dependency>
    <groupId>org.etd.framework</groupId>
    <artifactId>etd-framework-starter-storage</artifactId>
</dependency>
```

在 `application.yml` 中配置存储参数（按需开启对应的存储厂商）：

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

## 二、 后端 Java 接口说明与示例 (`FileUpload`)

### 1. 获取存储策略实例

```java
// 方式 A：自动注入被开启的策略 Bean
@Autowired
private FileUpload fileUpload;

// 方式 B：通过 StorageContext 动态调度获取 MinIO / OSS 策略 Bean
FileUpload minioStorage = StorageContext.getStorageClient(StorageContext.ClientType.MinIo);
FileUpload ossStorage = StorageContext.getStorageClient(StorageContext.ClientType.AlibabaOSS);
```

---

### 2. Web 前端直传能力 (HTTP PUT 方式，不占应用服务器带宽)

#### ① 单文件预签名直传 (`generateUploadUrl`)
```java
UploadUrlReqModel reqModel = new UploadUrlReqModel();
reqModel.setBucketName("avatar");
reqModel.setDirectory("2026/08");
reqModel.setOriginalFileName("photo.png");
reqModel.setContentType("image/png");

UploadUrlResModel res = fileUpload.generateUploadUrl(reqModel);
// res.getUploadUrl() -> 预签名直传 PUT URL
// res.getFileName()  -> 存盘相对 Key (如 "2026/08/A1B2C3.png")
```

#### ② 大文件分片直传 3 步 API
```java
// Step 1: 初始化分片任务
InitMultipartResModel initRes = fileUpload.initMultipart(initReq);

// Step 2: 为第 N 个分片生成直传 URL
GeneratePartUrlResModel partRes = fileUpload.generatePartUrl(partReq);

// Step 3: 所有分片直传成功后，通知云端合并
CompleteMultipartResModel compRes = fileUpload.completeMultipart(compReq);
```

---

### 3. 服务端直接上传能力 (在 JVM 内存中上传流/字节/文件)

#### ① 服务端 InputStream 流上传 (`upload`)
适用于后端导出 Excel、生成 PDF、网络图片转存等场景：

```java
InputStreamUploadReqModel reqModel = new InputStreamUploadReqModel();
reqModel.setBucketName("reports");
reqModel.setDirectory("2026/08");
reqModel.setOriginalFileName("sales_report.xlsx");
reqModel.setInputStream(excelInputStream); // 物理输入流

ServerUploadResModel res = fileUpload.upload(reqModel);
// res.getFileName() -> "2026/08/A1B2C3.xlsx"
// res.getFileUrl()  -> 在线访问外链
```

#### ② 服务端 byte[] 字节数组上传 (`upload`)
适用于内存二维码生成、字节快照上传等场景：

```java
ByteUploadReqModel reqModel = new ByteUploadReqModel();
reqModel.setBucketName("qrcode");
reqModel.setDirectory("user");
reqModel.setOriginalFileName("qr.png");
reqModel.setBytes(qrCodeBytes); // 内存字节数组

ServerUploadResModel res = fileUpload.upload(reqModel);
```

#### ③ 服务端 MultipartFile 上传 (`upload`)
适用于后端应用间调用的文件对象上传：

```java
MultipartUploadReqModel reqModel = new MultipartUploadReqModel();
reqModel.setBucketName("docs");
reqModel.setDirectory("contract");
reqModel.setFile(multipartFile);

ServerUploadResModel res = fileUpload.upload(reqModel);
```

---

## 三、 前端（Vue 3 / TypeScript / Axios）接入完整示例

### 场景一：单文件直传 (Small File Upload)

```typescript
import axios from 'axios';

export async function uploadSingleFile(file: File) {
  // 1. 问后端申请预签名直传地址
  const res = await axios.post('/api/file/upload-url', {
    bucketName: 'avatar',
    directory: 'user',
    originalFileName: file.name,
    contentType: file.type
  });

  const { uploadUrl, fileName } = res.data;

  // 2. 前端直接 PUT 文件二进制给 MinIO / 阿里云 OSS
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

  // 1. 初始化分片任务 (Init Multipart)
  const initRes = await axios.post('/api/file/multipart/init', {
    bucketName,
    directory,
    originalFileName: file.name,
    contentType: file.type
  });
  const { uploadId, fileName } = initRes.data;

  // 2. 本地切片并生成分片上传任务
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
  const partETags: Array<{ partNumber: number; eTag: string }> = [];

  for (let i = 0; i < totalChunks; i++) {
    const partNumber = i + 1;
    const start = i * CHUNK_SIZE;
    const end = Math.min(file.size, start + CHUNK_SIZE);
    const chunkBlob = file.slice(start, end);

    // 2.1 申请当前 Part 的直传地址
    const partUrlRes = await axios.post('/api/file/multipart/part-url', {
      bucketName,
      fileName,
      uploadId,
      partNumber
    });
    const { partUrl } = partUrlRes.data;

    // 2.2 前端直接将 Chunk Blob PUT 到云端
    const putRes = await axios.put(partUrl, chunkBlob, {
      headers: {
        'Content-Type': file.type || 'application/octet-stream'
      }
    });

    // 2.3 必须从响应头中读取 ETag 校验码
    const rawETag = putRes.headers['etag'] || putRes.headers['ETag'];
    const cleanETag = rawETag ? rawETag.replace(/"/g, '') : '';

    partETags.push({
      partNumber,
      eTag: cleanETag
    });
  }

  // 3. 所有分片完成后请求后端合并 (Complete Multipart)
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
