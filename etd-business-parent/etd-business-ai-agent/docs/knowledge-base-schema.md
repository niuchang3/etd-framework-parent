# 第一版知识库表结构设计

模型、知识库、目录、文档、分片及768/1024/1536维向量表的首次建库 DDL 已统一放入 `src/main/resources/sql/postgresql/schema.sql`，模型草稿、知识库、目录和原件文件管理接口已实现。模型管理复用 `ai_model_connection`、`ai_model_config`。第一版采用 PostgreSQL、pgvector、zhparser 及 Spring AI `PgVectorStore` 的写入约定；中英文全文索引和向量字段已在隔离库验证，应用层全文/向量/RRF混合检索仍待实现。当前部署使用包含 pgvector 和 zhparser 的 PostgreSQL 16 镜像，每个 AI 数据库先显式执行 `src/main/resources/sql/postgresql/extensions.sql`，再执行全量 `schema.sql`，不把全量初始化 DDL 当作增量迁移脚本重复执行。

当前已实现页面管理及原始文件分片上传，解析、文本分片、模型验证、向量化和检索暂不进入本次页面支持范围。前端复用上传组件、模块系统参数及实际上传接口约定见 [文件上传组件约定](file-upload-component.md)。本文保留检索阶段结构说明，不代表上述后续能力已实现或应在页面阶段提前开放。

## 表与关系

| 表 | 职责 |
| --- | --- |
| `ai_knowledge_base` | 租户知识库，维护检索模式、模型绑定、分片及检索默认参数 |
| `ai_knowledge_folder` | 知识库多级目录、目录排序及公开/角色数据权限 |
| `ai_knowledge_document` | 上传文件的目录、组织归属、公开状态、存储位置和处理状态 |
| `ai_file_upload` | 持久化上传任务、固定策略快照、原件替换版本和幂等完成结果 |
| `ai_storage_cleanup` | 与文件修改同事务提交的存储清理意图，后台加锁重试 |
| `ai_knowledge_chunk` | 分片标题、正文、来源位置、中英文全文索引、向量标识和处理状态 |
| `ai_vector_<dimensions>` | 该维度的分片正文检索副本、元数据与向量，按维度分表 |

一个模型配置可以绑定多个知识库，一个知识库有多个目录和文档，一个文档有多个分片。分片可以先保存、预览和调整，再发布检索；向量生成成功后才写入对应维度的向量表。全文索引放在维度无关的分片业务表，全文检索不调用 Embedding 模型，也不依赖向量生成成功。

当前 DDL 准备 `ai_vector_768`、`ai_vector_1024`、`ai_vector_1536`。768～1536 是允许的范围，不是只允许这三个档位；出现1280维等合法模型时，需要补充并部署对应表及索引后才能绑定，不能未经检查就写入相近维度表。

## 四张业务表的公共字段

沿用现有模型表及框架 `BaseEntity` 的基础字段，并增加乐观锁版本。业务表主键与向量表主键统一使用框架雪花算法分配的 `bigint`，不使用数据库自增或UUID主键；处理任务的 `processing_owner` 仍使用UUID作为租约标识，不属于实体主键。

| 字段 | PostgreSQL 类型 | 约束与含义 |
| --- | --- | --- |
| `id` | `bigint` | 主键，框架 `ASSIGN_ID` |
| `tenant_id` | `bigint` | 必填，从可信后端上下文取得 |
| `create_time` | `timestamp(6) with time zone` | 必填，默认当前时间，Java 使用 `Instant` |
| `create_by` | `bigint` | 创建用户标识，可空 |
| `update_time` | `timestamp(6) with time zone` | 必填，由应用在修改时刷新 |
| `update_by` | `bigint` | 修改用户标识，可空 |
| `data_status` | `integer` | 必填，默认 1；0禁用、1启用，复用 `BasicConstant.DataStatus` |
| `del_flag` | `smallint` | 必填，默认 0；0未删除、1已删除 |
| `version` | `bigint` | 必填，默认 0；非负，每次修改递增 |

目录和文档增加必填 `org_id`，实体继承框架 `OrgScopedEntity`，不重复声明组织字段；`create_by` 同时用于现有“本人数据”过滤。分片继承文档权限，不增加独立组织归属。知识库作为租户内业务容器，本次不增加独立知识库授权或组织读取门槛，避免再次阻断跨部门公开文件；知识库名称不应承载敏感内容，列表数量和摘要只统计当前用户可读内容。

当前已落地实体、管理接口及目录/文档权限SQL。公开读取SQL仅在对应Mapper方法跳过通用AND数据权限条件，并在SQL内显式组合相同角色范围；租户拦截始终保留。写操作继续走框架角色范围。向量、全文检索仍是后续能力，不能将本版SQL测试视为已开放检索接口。

## ai_knowledge_base：知识库

下列字段加上公共字段构成完整业务表。

| 字段 | PostgreSQL 类型 | 约束与含义 |
| --- | --- | --- |
| `knowledge_name` | `varchar(100)` | 必填，去除首尾空格，同租户未删除记录内唯一 |
| `description` | `varchar(1000)` | 可空，知识库用途说明 |
| `retrieval_mode` | `varchar(16)` | 必填，`keyword` 全文、`vector` 向量、`hybrid` 混合；基础阶段默认 `keyword`，保存其他模式只是草稿 |
| `embedding_model_config_id` | `bigint` | 引用本租户 `embedding` 类型的模型；基础阶段允许未绑定；当前页面API不开放绑定，未来启用向量/混合检索前必须验证模型 |
| `embedding_dimensions` | `integer` | 与模型绑定同时有值或同时为空；有值时为 768～1536，启用检索前需后端实测，页面只读，无统一默认维度 |
| `chunk_max_tokens` | `integer` | 必填，单个分片的目标 Token 上限，必须大于 0，并校验模型输入限制 |
| `chunk_overlap_tokens` | `integer` | 必填，相邻分片的重叠 Token 数，必须大于等于 0 且小于 `chunk_max_tokens` |
| `keyword_top_k` | `integer` | 必填，全文分支候选条数，必须大于 0；启用全文分支时读取 |
| `vector_top_k` | `integer` | 必填，向量分支候选条数，必须大于 0；启用向量分支时读取 |
| `retrieval_top_k` | `integer` | 必填，最终返回条数，必须大于 0；接口校验候选条数与最终条数的关系及数量上限 |
| `similarity_threshold` | `double precision` | 可空；仅用于向量分支，0～1 的 Spring AI 相似度阈值；不作用于全文得分或 RRF 融合得分 |

维度是知识库固定的存储属性，不能从每次请求传入。分片长度按 Token 计算，所用分词器和分片规则由后端固定实现并标识版本，不能把字符数误当作 Token 数。第一版不开放任意 Java 类名、脚本或无法校验的分片算法参数。

修改分片默认值只影响后续处理；已有文档必须显式重新分片，不能仅改知识库参数就宣称已生效。已有关联知识库的模型配置，禁止直接修改会改变向量空间的模型标识、服务端点、输出维度或相关参数；可以修改名称、凭证和超时等不改变向量语义的设置。换模型需要重新向量化，第一版不实现在线双版本切换。

纯全文知识库升级为向量或混合模式时，先绑定并测试模型、为已发布分片补齐向量，再切换有效检索模式。已有向量切为全文时可以保留有效模型绑定，但全文请求不调用模型；不能只修改模式字段就宣称历史数据具备新的检索能力。

## ai_knowledge_folder：多级目录

下列字段加上公共字段构成完整业务表，目录实体继承 `OrgScopedEntity`。

| 字段 | PostgreSQL 类型 | 约束与含义 |
| --- | --- | --- |
| `org_id` | `bigint` | 必填，目录归属组织；新增时默认取当前主组织，指定其他组织需后端校验 |
| `knowledge_base_id` | `bigint` | 必填，所属知识库，复合外键限定租户 |
| `parent_id` | `bigint` | 可空，空表示根目录；父目录必须在同租户同知识库，不能指向自身 |
| `folder_name` | `varchar(100)` | 必填，去首尾空格；同级未删除目录名称唯一，根目录同样不能重名 |
| `sort_order` | `integer` | 必填，默认0，非负；同级按该值、ID升序排列 |
| `is_public` | `boolean` | 必填，默认false；true只表示本租户内公开读取目录，不公开其文件或子目录 |

`parent_id`、`folder_id` 均使用 NULL 表示根目录或未分类，不使用0或虚构的目录记录。“全部文件”“未分类”是查询入口。目录关系采用邻接表与递归SQL，不额外保存易失真的路径或祖先冗余字段。

新增、移动时校验目标目录有效且可操作；文件、目录第一版只允许在同一知识库内移动。目录移动必须在同一事务内检查祖先链并串行化同一知识库的目录结构写操作，避免并发移动制造跨层循环；DDL只能拒绝自引用，不能独自阻止多节点循环。删除目录要求没有任何未删除子目录和文件，判断非空不能只统计当前用户可见条目。移动文件或目录不自动修改其组织归属和公开状态。

## 目录、文件与检索的统一权限

### 读取范围

第一版不增加负责人、访问范围枚举、用户/部门授权关系表，也不引入知识库成员体系。只使用目录、文档各自的 `is_public` 与框架现有角色数据范围：

```text
可信租户条件
AND 启用且未删除
AND（is_public = true OR 当前对象命中现有角色数据权限）
```

“本人”比较对象的 `create_by`；组织范围比较对象的 `org_id` 与后端已计算的 `scopeOrgIds`；多个角色仍按框架现有并集语义处理。缺少角色数据范围时非公开数据默认不可读，但已登录的本租户用户仍可读取公开资料。公开不等于匿名访问、不跨租户，也不授予修改、删除或发布权限。

平台管理员保留框架管理权限，AI接口显式限定当前操作租户，需在目标租户上下文操作；租户管理员跳过组织数据范围但仍受当前租户隔离。普通角色的“全部数据”范围可读取本租户内非公开资料，这是复用现有角色数据权限的明确结果，不能将它误称为逐文件保密授权。

### 目录树与文件的组合

- 目录和文档分别判定读取权限。公开目录不自动公开子目录或文件；非公开目录不阻止其中公开或命中数据权限的文件被检索、预览和下载。
- 目录树先取得直接可见目录、可读文件，再通过递归SQL补齐必要祖先链。仅用于导航的祖先只返回目录ID、父ID、名称及只读标志，不开放其备注、无权限子项或管理入口；补齐路径会展示必要父目录名称，这是本版已采用的导航语义。
- 目录没有可见子项、自己也不可读时不展示；直接可读的空目录可以展示。计数、分页总量、面包屑、搜索高亮及摘要均只基于可读内容，不能通过数量泄露隐藏文件。
- 目录/文件的新建、移动、替换、删除、公开状态及归属修改，需要框架接口write权限及原对象的管理数据范围，公开标志不扩大管理范围。目标目录也需校验写入权限；客户端不能借指定 `org_id` 扩大归属。
- 非空目录禁止删除，禁用非空目录第一版同样拒绝，避免产生不可达子项。批量操作逐项校验，不能凭目录可读或ID已存在就允许操作全部后代。

### MyBatis、全文与向量接入

当前通用 `EtdDataPermissionHandler` 会在外层 AND 追加组织/本人条件。本版目录/文件读取在特定Mapper方法中显式将同一角色范围表达式组合为 `(alias.is_public OR 角色范围条件)`，而不是仅手工增加公开条件后仍保留外层组织过滤。只对明确的知识库目录/文档读取查询采用此语义，不改变其他业务模块或写操作的既有权限规则；不能通过宽泛关闭租户或数据权限拦截来解决。

关联分片和向量时只对权威文档别名判定文件权限，不能又给目录、知识库、分片、向量一律追加 `org_id` 条件。全文和向量候选SQL都关联所属文档，在取Top K之前限制可信租户、文件可读性、有效知识库、当前处理轮次及发布状态。目录是分类，不是额外文件读取门槛。

权限结构示意，实际查询必须使用参数绑定，并由后端根据角色生成条件，不能接受前端SQL片段：

```sql
-- :tenant_id、:scope_org_ids、:user_id 及本人/组织范围开关全部来自可信后端上下文。
-- 全部数据、管理员等分支由权限能力生成；不能仅凭某个用户ID就启用“本人”范围。
WHERE d.tenant_id = :tenant_id
  AND d.del_flag = 0 AND d.data_status = 1
  AND (d.is_public = true OR <框架现有角色数据范围表达式>)
```

该片段只解释权限组合，不是可直接运行的SQL，也不是已实现接口。原生 `PgVectorStore` JDBC读取不会经过MyBatis权限拦截，因此不能直接使用默认 `similaritySearch` 作为业务检索入口。预签名下载链接必须在签发前执行相同文件读取校验，链接短期有效，已签发链接不会因数据库权限修改而即时撤销。后续权限撤销应及时失效相关缓存，不能依赖向量重建或旧缓存权限。

## ai_knowledge_document：知识库文档

下列字段加上公共字段构成完整业务表。

| 字段 | PostgreSQL 类型 | 约束与含义 |
| --- | --- | --- |
| `knowledge_base_id` | `bigint` | 必填，引用本租户知识库 |
| `org_id` | `bigint` | 必填，文件归属组织，接入 `OrgScopedEntity` |
| `folder_id` | `bigint` | 可空表示未分类；复合外键保证目录在同租户同知识库 |
| `is_public` | `boolean` | 必填，默认false；公开或命中角色数据范围时可读，不继承目录值 |
| `document_name` | `varchar(255)` | 必填，页面显示的文档名称，可与原文件名不同 |
| `original_filename` | `varchar(255)` | 必填，上传时的原始文件名，仅供展示，不用于拼接本地路径 |
| `storage_provider` | `varchar(32)` | 必填，文件存储实现；第一版为 `minio` |
| `storage_bucket` | `varchar(255)` | 必填，原始文件所在桶 |
| `storage_object_key` | `text` | 必填，原始文件对象键，由后端生成并包含租户边界，不保存临时签名 URL |
| `content_type` | `varchar(255)` | 必填，后端按原始文件名推断的 MIME 类型，未知类型为 application/octet-stream |
| `file_size` | `bigint` | 必填，字节数，必须大于 0 |
| `file_sha256` | `varchar(64)` | 必填，文件内容的 SHA-256 十六进制摘要；不设置跨租户唯一约束 |
| `processing_status` | `varchar(32)` | 必填，`uploaded`、`pending`、`parsing`、`preview`、`embedding`、`ready`、`failed` |
| `processing_version` | `bigint` | 必填，默认 0；每次重新处理递增，用于拒绝旧任务回写 |
| `processing_owner` | `uuid` | 可空，当前处理任务的唯一租约标识，与租约到期时间成组写入和清空 |
| `processing_lease_until` | `timestamp(6) with time zone` | 可空，当前处理任务租约到期时间；解析/向量化状态必须持有租约 |
| `processing_started_time` | `timestamp(6) with time zone` | 可空，本轮处理开始时间，租约到期时间必须晚于开始时间 |
| `parser_version` | `varchar(64)` | 可空，成功解析时记录实际解析器及规则版本 |
| `splitter_version` | `varchar(64)` | 可空，分片完成时记录实际分词器及分片规则版本 |
| `chunk_max_tokens` | `integer` | 可空，本轮处理实际使用的分片上限快照，分片后必填 |
| `chunk_overlap_tokens` | `integer` | 可空，本轮处理实际使用的重叠数快照，分片后必填 |
| `error_message` | `varchar(1000)` | 可空，面向用户的脱敏错误摘要，失败时填写，禁止保存密钥或模型请求全文 |
| `processed_time` | `timestamp(6) with time zone` | 可空，本轮处理成功或失败的结束时间点 |

`preview` 表示分片及全文索引已生成，可预览但尚未发布检索；纯全文模式确认后可直接进入 `ready`，向量/混合模式还需完成向量化。`ready` 表示满足当前模式的发布要求，不等同于任何模式下都已生成向量。状态与失败详情由工作流维护，不能用基础 `data_status` 代替处理进度。

进入 `parsing` 或 `embedding` 时，在同一条件更新中写入租约标识、开始时间、到期时间和处理轮次；续租、写入及最终发布都检查租约标识、轮次与有效期。任务异常退出后，后端只允许抢占已过期租约，抢占时递增处理轮次并撤销旧检索资格；发布、失败或进入预览时清空租约标识及到期时间。DDL 只约束字段组合，不自动执行抢占、续租或重试，业务实现仍需处理旧任务晚到写入和清理。

后续解析阶段从保存的分片正文预览内容，需要重新解析时读取 MinIO 原件，不额外维护一份全量解析正文。文件下载和预览均由后端校验所属租户、知识库、状态以及“公开或角色数据范围”后授权。对象键不保存可长期公开访问的 URL。

## ai_knowledge_chunk：文档分片

下列字段加上公共字段构成完整业务表。

| 字段 | PostgreSQL 类型 | 约束与含义 |
| --- | --- | --- |
| `knowledge_base_id` | `bigint` | 必填，与所属文档的知识库一致，用于限定检索与复合关联 |
| `document_id` | `bigint` | 必填，引用同租户、同知识库的文档 |
| `processing_version` | `bigint` | 必填，创建该分片的文档处理版本 |
| `chunk_index` | `integer` | 必填，本轮文档内的分片顺序，从 0 开始 |
| `title` | `text` | 必填，默认空字符串，当前分片的章节标题；无章节标题时使用文档显示名称 |
| `content` | `text` | 必填，分片正文，作为预览、编辑和向量化的权威来源 |
| `search_vector` | `tsvector` | 数据库生成的 STORED 列；由标题、正文的中文分词和英文规范化结果组成，标题权重高于正文 |
| `token_count` | `integer` | 必填，按实际分词器统计的 Token 数，必须大于 0 |
| `source_location` | `jsonb` | 必填，默认 `{}`；按格式记录页码、标题路径等来源信息，不保存任意执行参数 |
| `vector_id` | `bigint` | 必填且唯一，框架雪花算法预分配，作为 Spring AI `Document.id` 和向量表主键，不使用数据库自增 |
| `embedding_status` | `varchar(16)` | 必填，`not_required`、`pending`、`ready`、`failed`；纯全文模式尚未启用向量时使用 `not_required` |
| `error_message` | `varchar(1000)` | 可空，当前分片向量化失败的脱敏摘要 |
| `embedded_time` | `timestamp(6) with time zone` | 可空，当前正文成功生成并写入向量的时间点 |

每轮重新分片生成新分片和新 `vector_id`，旧处理版本不得继续被检索。预览阶段可编辑标题、正文并重新计算 Token 数，全文生成列随写入同步刷新。已发布分片编辑前先撤下其全文与向量检索资格，完成当前模式要求后再发布。异步写入前和发布完成前均检查处理版本，并使用任务互斥或等价机制避免旧任务在清理后重新插入旧向量；仅有版本字段或缓存版本不能自行提供这一保证。

业务列表默认只显示当前文档处理版本的分片；旧轮次数据完成清理后物理移除，不建设未确认的历史版本功能。第一版重新处理前先撤下旧向量，允许处理期间暂不可检索，不承诺在线无损切换。

## 中英文全文检索与混合检索

### 分词与字段生成

全文索引属于分片，不按向量维度重复建设。`search_vector` 使用统一的双语处理规则，而不是要求用户为每份混合语言文档手选一种语言：

- 中英文统一经过 `zhparser` 与 SCWS 切词，使用固定命名的 `public.ai_zh_v1` 文本搜索配置，不再拆成两套语言查询后 OR 拼接。
- 切词结果统一经过 PostgreSQL 内置 `english_stem` 词典：中文保留词项，英文进行大小写、停用词和词干处理，例如 `running` 与 `run` 归一化。入库和查询必须使用同一配置。
- 对标题、正文分别生成统一中英文 `tsvector`，标题使用 `A` 权重，正文使用 `B` 权重，保留词项位置以供 `ts_rank_cd` 排序。第一版不开放用户查询语法或额外短语检索能力。
- 中文搜索配置需要覆盖目标语料里的常见词、专名、英文/数字混合标识及未知词，并通过 `ts_debug` 验证；不能照搬一个仅保留名词的示例映射后就宣称中英文技术文档全部可搜。

生成列的表达式必须显式指定 `regconfig`，不得依赖连接级 `default_text_search_config`。新增或修改中文词库、映射规则、英文规范化或标题来源时，需要重新计算全文生成列并维护其 GIN 索引；仅执行 `REINDEX` 不会重新生成已存储的词项。

`zhparser` 是数据库扩展，Deployment 使用包含 pgvector、zhparser、SCWS 和词库文件的镜像。通过隔离 PostgreSQL 16 验证 `ai-agent` 库名、中文切词、英文词干和中英混排。每个 AI 库仍需执行 `extensions.sql`，安装扩展文件不代表库内已经启用；当前只使用镜像内置词库，不把固定词库测试结果扩展为任意租户自定义词库均可使用的承诺。

### 查询与排序

| 模式 | 召回方式 | 是否调用模型 |
| --- | --- | --- |
| `keyword` | 中文查询分词、英文查询规范化，匹配 `search_vector` 后按 `ts_rank_cd` 排序 | 不调用 |
| `vector` | `PgVectorStore` 使用绑定的 Embedding 模型生成查询向量，并进行相似度检索 | 调用 Embedding |
| `hybrid` | 分别召回全文和向量候选，按业务分片 ID 去重，使用 RRF 融合排序 | 向量分支调用 Embedding |

全文查询通过 `public.ai_search_query` 使用同一分词配置生成参数化 `plainto_tsquery`。已覆盖正文“智能中文知识库 run”与查询“中文知识库 running”的组合回归；不能仅分别测试中文和英文成功就认定混排查询正确。查询词项为空时全文分支返回空候选，不得移除过滤条件返回全库。第一版不将用户文本直接拼接成 `to_tsquery` 表达式，不承诺搜索引擎式的任意查询语法。

全文查询必须在 SQL 中 JOIN 所属文档、知识库并限定可信租户、授权知识库、当前 `processing_version`、启用及逻辑删除状态，并要求文档 `processing_status='ready'`。不能先在全库截取 Top K，再在 Java 中过滤租户或拼接文档；这些 JOIN 与 WHERE 所需索引见下节。

全文 `ts_rank_cd` 不是 BM25，其得分也不与向量相似度处在同一尺度。混合检索第一版采用等权 RRF：对候选在各分支中的排名贡献 `1 / (k + rank)`，将相同分片的贡献求和排序，`rank` 从 1 开始。`k` 作为明确的后端策略常量统一定义，不额外开放两个原始分数直接加权的页面参数；不能把最终融合得分解释为 0～1 的置信度。

全文候选与向量候选都先落实权限和生命周期过滤，再参与融合；融合完成后再次校验当前发布版本，避免并发删除或重建的数据进入 Agent 上下文。向量查询中的过滤不足不能由最终校验替代召回性能验证。

### Spring AI Alibaba 的接入边界

`EmbeddingModel` 负责生成向量，向量写入遵循 `PgVectorStore` 的四字段约定；实际召回需使用能 JOIN 业务发布状态的 Mapper SQL，在租户、授权知识库、当前处理版本、启停及删除条件过滤之后取 Top K。原生 `PgVectorStore.similaritySearch` 只查向量表，不能代替这些业务关联过滤。中英文全文召回由独立的能力 Service/Mapper SQL 承担；完整检索用例由知识库 `biz` 编排，输出统一分片结果并转换为 Spring AI `Document` 供 RAG 或 Agent 使用。

Spring AI 1.1.2 的 `PgVectorStore.similaritySearch` 不会自动执行中文全文检索或 RRF。全文生成列放在业务分片表，保持原生 `PgVectorStore` 的四字段写入约定，避免为了增加全文字段而要求其插入不认识的必填列。

## ai_vector_<dimensions>：Spring AI 向量表

例如 `ai_vector_1024`，字段结构与 Spring AI 1.1.2 的 `PgVectorStore` 插入、更新和查询保持一致。

| 字段 | PostgreSQL 类型 | 约束与含义 |
| --- | --- | --- |
| `id` | `bigint` | 主键，等于业务分片的雪花 `vector_id`，由后端提供，无自增序列或数据库默认值 |
| `content` | `text` | 必填，已发布分片正文的检索副本，供 Spring AI 返回 `Document` |
| `metadata` | `jsonb` | 必填，由后端构造的隔离、关联及来源元数据 |
| `embedding` | `vector(1024)` | 必填，维度必须恰好匹配表名；其他维度表只替换这个长度 |
| `tenant_id`、`knowledge_base_id`、`document_id`、`chunk_id`、`model_config_id`、`processing_version` | `bigint` | 从 `metadata` 生成的 STORED 非空列，参与复合外键及索引，不由 Spring AI 直接写入 |
| `embedding_dimensions` | `integer` | 按本表固定维度生成的 STORED 列，用于校验知识库模型绑定 |

向量表是检索投影，不继承 `BaseEntity`。后七列由数据库生成，`PgVectorStore` 仍只需写四个基础字段；不能手工给生成列赋值。两个复合外键分别校验分片的租户、知识库、文档、业务ID、处理轮次、雪花向量ID，以及知识库的绑定模型和维度。数据库因此能够拒绝跨租户、跨知识库或维度路由错误的向量写入。

Spring AI 1.1.2 的长整型ID映射枚举名为 `PgVectorStore.PgIdType.BIGSERIAL`，其运行时通过 `Long.valueOf` 读取 `Document.id`。后续创建客户端时必须显式配置 `.idType(PgVectorStore.PgIdType.BIGSERIAL).initializeSchema(false)`；这里仅使用它的长整型映射，不让它创建 `bigserial` 列或序列。物理表保持本工程显式DDL中的普通 `bigint`，并通过 `Document.builder().id(Long.toString(vectorId))` 传入已分配的雪花ID，不使用默认生成的UUID或32位 `INTEGER` 映射。

元数据的必填键如下，所有业务 ID 与版本统一保存为十进制字符串，避免 JSON 消费方数值精度及字符串/数字混用：

```json
{
  "tenant_id": "10001",
  "knowledge_base_id": "20001",
  "document_id": "30001",
  "chunk_id": "40001",
  "model_config_id": "50001",
  "processing_version": "1"
}
```

这些键只由后端填写，并以数据库 CHECK 约束校验必填、JSON 字符串类型及十进制格式；不能依赖前端自由填写元数据实现隔离。检索强制组合可信租户、授权知识库、模型配置过滤。即使两个模型同为 1024 维，也不允许跨模型空间检索。

`PgVectorStore` 的 JDBC 操作不会经过 MyBatis 租户拦截器。读取、写入、删除都必须经过知识库业务边界校验；特别是按 ID 删除前校验分片归属，不能将未校验的雪花ID列表直接交给默认删除方法。停用、删除知识库/文档/分片时，同步撤下对应向量；不能只修改业务表的逻辑删除标识。检索结果还需按当前业务状态及处理版本授权校验，失效数据不得进入 Agent 上下文。

## 约束与索引方案

以下约束和索引已在全量 `schema.sql` 中落地；运行时生命周期和权限校验仍由业务实现承担。

| 对象 | 约束或索引 |
| --- | --- |
| 模型配置 | 非部分唯一约束 `(tenant_id,id,embedding_dimensions)`，供知识库复合外键引用 |
| 知识库引用模型 | `(tenant_id,embedding_model_config_id,embedding_dimensions)` 复合外键及同顺序 B-tree 索引；模型类型CHECK保证只有Embedding能提供非空维度，启停与软删除仍由业务校验 |
| 知识库 | 唯一约束 `(tenant_id,id)`；未删除记录的 `(tenant_id,knowledge_name)` 唯一索引 |
| 知识库列表 | 部分 B-tree 索引 `(tenant_id,create_time desc,id desc) where del_flag=0` |
| 目录引用知识库、父目录 | `(tenant_id,knowledge_base_id)` 与 `(tenant_id,knowledge_base_id,parent_id)` 复合外键，拒绝跨租户、跨知识库及自引用 |
| 同级目录重名 | 部分唯一索引 `(tenant_id,knowledge_base_id,parent_id,folder_name) NULLS NOT DISTINCT WHERE del_flag=0`，根目录同样唯一 |
| 目录树与父外键 | 非部分 B-tree `(tenant_id,knowledge_base_id,parent_id,sort_order,id)`，支持递归关联与同级排序 |
| 目录读取范围 | 未删除、启用记录的公开部分索引及组织/创建人索引，分别支持公开、组织、本人分支 |
| 文档引用目录及目录列表 | `(tenant_id,knowledge_base_id,folder_id)` 复合外键；非部分 B-tree `(tenant_id,knowledge_base_id,folder_id,create_time desc,id desc)`，覆盖外键检查和目录分页 |
| 文档读取范围 | 未删除、启用记录的公开部分索引及组织/创建人分页索引；正式权限OR查询需验证执行计划 |
| 文档引用知识库 | `(tenant_id,knowledge_base_id)` 复合外键 |
| 文档列表 | 部分 B-tree 索引 `(tenant_id,knowledge_base_id,create_time desc,id desc) where del_flag=0` |
| 文档被分片引用 | 非部分唯一约束 `(tenant_id,knowledge_base_id,id)` |
| 分片引用文档 | `(tenant_id,knowledge_base_id,document_id)` 复合外键，防止同租户跨知识库挂接 |
| 分片顺序及外键检查 | 唯一约束 `(tenant_id,knowledge_base_id,document_id,processing_version,chunk_index)`，同时支持按顺序读取一轮分片 |
| 分片映射 | `vector_id` 唯一约束，用于检索结果关联与按向量 ID 清理 |
| 向量引用分片 | 分片非部分唯一键 `(tenant_id,knowledge_base_id,document_id,id,processing_version,vector_id)` 与向量表生成列的复合外键 |
| 向量引用知识库绑定 | 知识库非部分唯一键 `(tenant_id,id,embedding_model_config_id,embedding_dimensions)` 与向量表对应生成列的复合外键 |
| 分片全文匹配 | 部分 GIN 索引 `using gin(search_vector) where del_flag=0 and data_status=1`；查询带相同生效条件，发布状态由关联文档控制 |
| 全文 JOIN 与范围过滤 | 分片已有 `(tenant_id,knowledge_base_id,document_id,processing_version,chunk_index)` 唯一索引，文档与知识库的复合唯一键支持关联；正式 SQL 需检查 `EXPLAIN (ANALYZE,BUFFERS)` |
| 向量近邻搜索 | 每张实际维度表建立 `embedding vector_cosine_ops` 的 HNSW 索引 |
| 向量关联和批量清理 | 生成列 B-tree `(tenant_id,knowledge_base_id,document_id,chunk_id,processing_version,id)` 及 `(tenant_id,knowledge_base_id,model_config_id,embedding_dimensions)`，覆盖两个复合外键的引用键 |
| 向量元数据过滤 | GIN `metadata jsonb_path_ops` 支持原生 Spring AI JSON path 条件；实际检索还需验证执行计划，不代表其替代了业务状态JOIN |
| 文档租约恢复扫描 | 部分索引 `(processing_lease_until,tenant_id,id)`，限定未删除且持有租约的解析/向量化任务 |

外键检查使用的索引覆盖包括逻辑删除在内的全部引用记录。文档的复合唯一键以 `(tenant_id,knowledge_base_id)` 开头，分片的顺序唯一键以 `(tenant_id,knowledge_base_id,document_id)` 开头，已覆盖各自外键检查，无需再建重复前缀索引。数据库外键不自动阻止逻辑删除，删除流程仍需校验关联对象。

HNSW 不会自动与租户 B-tree/GIN 索引组合为过滤前的分组近邻索引。必须使用真实多租户数据，检查限制知识库后的召回条数、执行计划与延迟，再决定是否启用迭代扫描或调整索引划分，不能声称添加元数据索引就解决了全部过滤性能问题。

Spring AI 初始化设置为关闭，DDL 由部署流程执行。动态 `PgVectorStore` 使用经过后端整数范围校验的维度、固定表名前缀和已部署表清单选择表名，不能接收用户原样提供的 SQL 标识符；按租户模型配置及版本复用实例，避免共享实例切换模型。

## 第一版实施边界

### 页面状态字典

状态标签与筛选项复用 UPMS 全局内置字典，初始化脚本放在 AI 工程的 `src/main/resources/sql/postgresql/data.sql`。先完成 UPMS 字典表建表，再连接系统字典所在数据库显式执行；AI 数据库不重复维护字典表，应用启动时不自动执行。

| 字典类型编码 | 对应字段 | 字典值 |
| --- | --- | --- |
| `ai_knowledge_document_status` | 文档 `processing_status` | `uploaded`、`pending`、`parsing`、`preview`、`embedding`、`ready`、`failed` |
| `ai_knowledge_chunk_embedding_status` | 分片 `embedding_status` | `not_required`、`pending`、`ready`、`failed` |
| `ai_knowledge_retrieval_mode` | 知识库 `retrieval_mode` | `keyword`、`vector`、`hybrid` |

页面通过 UPMS 已有的 `GET /v1/dict/type/code/{typeCode}/data` 获取标签和筛选选项，状态转换由后端工作流控制，不提供任意修改状态的下拉保存操作。后续生成业务枚举和建表 CHECK 时必须复用这些显式代码。

知识库绑定模型的下拉列表查询当前租户可用的 Embedding 模型，不从字典枚举实际模型。向量维度为实测结果，文件存储由框架配置选择；第一版不增加维度、厂商型号或存储服务的用户选择字典。

当前实现基础管理和文件传输，不增加聊天会话、Agent编排、计费、模型市场、独立AI任务中心、平台模型授权或ES存储；文件传输会话与存储清理队列已经实现。后续平台提供模型时再落实已确认的授权与计费规则。未来转 ES 时，业务表仍作为知识库、文件与分片的权威来源，通过向量存储边界完成迁移，不提前保留双写链路。

接口与字段的实际前端合同见 [前端AI实施提示词](frontend-ai-prompt.md)。基础阶段模型维度可为空，文档新状态为uploaded，二者均不表示已完成模型验证或解析。完整隔离验证入口是 `scripts/verify-foundation.py`。

核对依据：[Spring AI 1.1.2 PgVectorStore 源码](https://github.com/spring-projects/spring-ai/blob/v1.1.2/vector-stores/spring-ai-pgvector-store/src/main/java/org/springframework/ai/vectorstore/pgvector/PgVectorStore.java)、[PostgreSQL 16 全文索引](https://www.postgresql.org/docs/16/textsearch-tables.html)、[zhparser 官方说明](https://github.com/amutu/zhparser)。隔离容器验证加载真实全量DDL及 `src/test/resources/sql/postgresql/knowledge-schema-smoke.sql`，验证目录同级重名与跨范围约束、公开/组织/本人读取表达式、祖先导航、跨租户约束、维度绑定、Spring AI式四字段写入、全文与向量查询、发布轮次过滤及列注释；尚未实现模型调用、解析任务租约调度或混合检索；真实UPMS账号及浏览器页面联调仍需后续验证。
