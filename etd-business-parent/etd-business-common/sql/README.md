# 数据库初始化脚本

所有 SQL 文件统一维护在 `etd-business-common/sql/`，不按工程创建子目录。

| 文件 | 内容 | 执行数据库 |
| --- | --- | --- |
| `upms.sql` | UPMS 建表语句 | UPMS |
| `event.sql` | Event 建表语句，业务应用与 Event server 共用唯一版本 | Event |
| `ai.sql` | AI 模型、知识库、分类、资料、分片和三种维度向量表定义 | AI |
| `ai_data.sql` | 经官方文档核实的模型管理初始数据，不含租户连接和凭证 | AI |
| `oauth.sql` | OAuth 授权服务建表语句 | 授权服务实际使用的数据库 |
| `init.sql` | UPMS、AI、Event 的公共字典、菜单及系统参数，统一初始化 | UPMS |
| `upms_data.sql` | UPMS 专属数据：默认租户、管理员、角色、组织及默认租户菜单授权 | UPMS |

UPMS 初始化顺序：`upms.sql` → `init.sql` → `upms_data.sql`。公共菜单定义与特定租户的授权关系分开维护，后者属于 `upms_data.sql`。

Event、AI、OAuth 分别在对应数据库执行建表脚本，不把本目录所有文件依次执行到同一个数据库。AI 初始化顺序为 `ai.sql` → `ai_data.sql`。`ai.sql` 创建模型、知识库、库内分类、资料上传、分片和 768/1024/1536 维向量表，使用 zhparser 为分片正文建立中文全文 GIN 索引；执行前须在 PostgreSQL 服务端安装 zhparser 及其 SCWS 依赖，执行账号须有创建扩展权限。同时依赖 pgvector 扩展，执行前须安装。全文查询须使用 `public.etd_knowledge_to_tsvector(content) @@ public.etd_knowledge_to_tsquery(?)` 并包含 `del_flag = 0` 条件；两个分词函数固定启用短词复合，保持索引和查询设置一致。

脚本由部署人员显式执行，不随应用启动自动运行。工程资源目录不保留 SQL 副本；业务父 POM 仅将本目录的 `*.sql` 加入测试资源，测试使用 `classpath:sql/ai.sql`、`classpath:sql/event.sql` 等平铺路径，不进入应用运行资源。
