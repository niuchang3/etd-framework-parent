# 本地学习用 MinIO

单实例 MinIO，使用默认 StorageClass 申请 5Gi PVC。凭证优先从可选的 `minio-secret` 注入容器环境变量；变量缺失或为空时，使用用户指定的本地学习默认值：用户名 `root`、密码 `12345678`。当前本地 OrbStack 提供默认 `local-path` StorageClass；其他集群先用 `kubectl get storageclass` 确认默认存储可用。

镜像从官方源码 `RELEASE.2025-10-15T17-29-55Z` 构建，API 与 Console 分别监听 9000、9001。原官方 Quay 镜像在初次接入验证时无法匿名拉取，因此使用[官方发布说明](https://github.com/minio/minio/releases/tag/RELEASE.2025-10-15T17-29-55Z)中的源码安装方式构建，不依赖第三方重新打包的 MinIO 镜像。本目录用于本地学习。

本目录只保存部署清单及使用说明，文件沿用 `<组件名>-deployment.yaml`、`<组件名>-service.yaml` 命名；`minio-pvc.yaml` 用于持久化数据。镜像构建文件单独放在 [docker/minio/Dockerfile](../../docker/minio/Dockerfile)。

## 构建本地镜像

在仓库根目录执行：

```bash
docker build -t etd/minio:RELEASE.2025-10-15T17-29-55Z docker/minio/
```

首次构建需要下载 Go 工具链镜像和源码依赖，后续构建复用缓存。当前 Docker 和 Kubernetes 均使用 OrbStack，[两者共享镜像存储](https://docs.orbstack.dev/kubernetes/#container-images)，构建完成后即可部署，不需要推送镜像。其他 Kubernetes 环境须先将该镜像导入节点，或推送到可访问的镜像仓库并修改 Deployment 的镜像地址。

## 使用默认凭证部署

在仓库根目录执行。先确认 `kubectl config current-context` 是预期的本地集群：

```bash
kubectl apply -f k8s-configs/minio/
kubectl rollout status deployment/minio-deployment --timeout=180s
```

无需提前创建 Secret；没有自定义配置时，使用 `root / 12345678` 登录。若当前命名空间中已有 `minio-secret`，其中非空的账号密码会优先于默认值。

## 自定义账号密码

Kubernetes 不会自动读取本机终端的环境变量，需要通过以下命令创建或更新 Secret。Secret、Deployment、Service 和 PVC 必须在同一命名空间。

交互输入账号密码，密码输入不回显；用户名至少 3 个字符，密码至少 8 个字符：

```bash
printf 'MinIO 用户名: '
read -r MINIO_ROOT_USER
printf 'MinIO 密码: '
read -rs MINIO_ROOT_PASSWORD
printf '\n'

kubectl create secret generic minio-secret \
  --from-literal=MINIO_ROOT_USER="${MINIO_ROOT_USER:?请输入用户名}" \
  --from-literal=MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:?请输入密码}" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -f k8s-configs/minio/
kubectl rollout status deployment/minio-deployment --timeout=180s
```

修改 Secret 后，执行 `kubectl rollout restart deployment/minio-deployment` 使容器重新读取凭证。不要把包含真实密码的 Secret YAML 或环境文件提交到仓库。

## 访问与检查

| 用途 | OrbStack 本机地址（default 命名空间） | 同命名空间的集群内地址 |
| --- | --- | --- |
| S3 API / SDK | `http://minio-service.default.svc.cluster.local:9000` | `http://minio-service:9000` |
| 浏览器控制台 | `http://minio-service.default.svc.cluster.local:9001` | `http://minio-service:9001` |

OrbStack 支持从 Mac 直接访问上述 Service 域名；使用其他命名空间时，将域名中的 `default` 替换为实际命名空间。NodePort 使用节点 IP 访问：API 为 `http://<节点IP>:30900`，控制台为 `http://<节点IP>:30901`，节点 IP 可通过 `kubectl get nodes -o wide` 查看。当前环境的 `localhost:30900/30901` 不会自动转发到 NodePort。

在控制台使用实际生效的账号密码登录（未配置时为 `root / 12345678`），创建私有存储桶 `ai-agent`，即可用于文件上传下载练习。

```bash
kubectl get pods,pvc,svc -l app=minio
kubectl get pvc minio-data
curl --fail http://minio-service.default.svc.cluster.local:9000/minio/health/ready
```

如果所在本地 Kubernetes 环境不把 NodePort 转发到 localhost，可以运行：

```bash
kubectl port-forward service/minio-service 9000:9000 9001:9001
```

此时本机 API 与控制台地址分别为 `http://localhost:9000`、`http://localhost:9001`，保持该命令运行。

## 接入智能体应用

使用默认凭证时，在应用启动终端中设置：

```bash
export AI_STORAGE_MINIO_ENDPOINT=http://minio-service.default.svc.cluster.local:9000
export AI_STORAGE_MINIO_ACCESS_KEY=root
export AI_STORAGE_MINIO_SECRET_KEY=12345678
export AI_STORAGE_MINIO_BUCKET=ai-agent
```

如果通过 Secret 配置了自定义账号密码，这两个变量也必须改为实际生效的值。

使用 `port-forward` 时，改用 `http://localhost:9000`。应用本身部署在同一命名空间时，使用 `http://minio-service:9000`；如果要把预签名 URL 直接交给本机浏览器访问，还需确保签名所用地址可由浏览器解析并访问。

另外通过环境注入 `AI_DASHSCOPE_API_KEY`，按[智能体应用说明](../../etd-business-parent/etd-business-ai-agent/README.md)构建，然后使用默认启用 MinIO 的 local 环境：

```bash
java -jar etd-business-parent/etd-business-ai-agent/target/etd-business-ai-agent-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

SDK 连接 S3 API 端口，不要填写控制台端口。

## 停止服务并保留文件

```bash
kubectl delete -f k8s-configs/minio/minio-deployment.yaml
kubectl delete -f k8s-configs/minio/minio-service.yaml
```

PVC 和 Secret 保留，下次部署可以继续使用。删除 PVC 可能触发底层存储回收；不要通过 `kubectl delete -f k8s-configs/minio/` 停止服务，否则会同时删除本目录声明的 PVC。
