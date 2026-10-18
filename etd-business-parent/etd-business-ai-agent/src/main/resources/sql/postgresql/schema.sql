/*==============================================================*/
/* SaaS 模型、知识库、分片及向量检索的完整初始结构。           */
/* 本脚本描述当前最终结构，由部署流程显式执行，不在应用启动时执行。 */
/*==============================================================*/

-- 先执行 extensions.sql；本文件为首次建库 DDL，不作为增量迁移脚本重复执行。
begin;
set local search_path = public, pg_catalog;

create table ai_model_connection
(
    id                    bigint not null,
    create_time           timestamp(6) with time zone not null default current_timestamp,
    create_by             bigint,
    update_time           timestamp(6) with time zone not null default current_timestamp,
    update_by             bigint,
    data_status           integer not null default 1,
    tenant_id             bigint not null,
    del_flag              smallint not null default 0,
    version               bigint not null default 0,
    connection_name       varchar(100) not null,
    provider              varchar(32) not null,
    base_url              varchar(1000) not null,
    auth_type             varchar(16) not null,
    api_key_ciphertext    text,
    credential_key_id     varchar(64),
    remark                varchar(500),
    primary key (id),
    -- 复合外键的被引用键必须包含租户，不能使用仅覆盖未删除记录的部分唯一索引。
    constraint uk_ai_model_connection_tenant_id unique (tenant_id, id),
    constraint ck_ai_model_connection_status check (data_status in (0, 1)),
    constraint ck_ai_model_connection_deleted check (del_flag in (0, 1)),
    constraint ck_ai_model_connection_version check (version >= 0),
    constraint ck_ai_model_connection_name check
        (connection_name = btrim(connection_name) and connection_name <> ''),
    constraint ck_ai_model_connection_provider check
        (provider in ('dashscope', 'ollama', 'openai_compatible')),
    constraint ck_ai_model_connection_url check (base_url ~ '^https?://[^[:space:]]+$'),
    constraint ck_ai_model_connection_auth check
        ((auth_type = 'none' and api_key_ciphertext is null and credential_key_id is null)
         or (auth_type = 'api_key' and nullif(btrim(api_key_ciphertext), '') is not null
             and nullif(btrim(credential_key_id), '') is not null))
);

comment on table ai_model_connection is '租户模型服务连接；同一连接可供多个模型配置复用';
comment on column ai_model_connection.id is '应用分配的全局主键，沿用 BaseEntity 的 ASSIGN_ID';
comment on column ai_model_connection.create_time is '创建时间点，Java 使用 Instant';
comment on column ai_model_connection.create_by is '创建用户标识';
comment on column ai_model_connection.update_time is '修改时间点，由应用在每次修改时更新';
comment on column ai_model_connection.update_by is '修改用户标识';
comment on column ai_model_connection.data_status is '启停状态：0禁用、1启用，对应 BasicConstant.DataStatus';
comment on column ai_model_connection.tenant_id is '所属租户标识，取自后端可信租户上下文';
comment on column ai_model_connection.del_flag is '逻辑删除标识：0未删除、1已删除';
comment on column ai_model_connection.version is '乐观锁及客户端配置版本；每次修改递增，包含地址、密钥和状态修改';
comment on column ai_model_connection.connection_name is '租户自定义连接名称，去除首尾空格后保存，同租户未删除记录内唯一';
comment on column ai_model_connection.provider is '接入适配器：dashscope、ollama、openai_compatible；不代表模型类型';
comment on column ai_model_connection.base_url is '服务端可访问的 API 根地址；不允许携带凭证，协议路径由对应适配器解释';
comment on column ai_model_connection.auth_type is '认证方式：none无需认证，api_key使用加密保存的 API Key';
comment on column ai_model_connection.api_key_ciphertext is 'API Key 的认证加密密文封装，包含格式版本、随机 nonce 和认证标签；禁止保存或返回明文';
comment on column ai_model_connection.credential_key_id is '服务端加密密钥的非敏感标识；实际主密钥通过运行环境提供，不存入本表';
comment on column ai_model_connection.remark is '连接备注，禁止填写 API Key 等敏感信息';

create unique index uk_ai_model_connection_tenant_name
    on ai_model_connection (tenant_id, connection_name)
    where del_flag = 0;

-- 租户连接列表按创建时间倒序分页，不使用应用侧集合过滤或排序。
create index idx_ai_model_connection_tenant_created
    on ai_model_connection (tenant_id, create_time desc, id desc)
    where del_flag = 0;

/*==============================================================*/
/* table: ai_model_config                                       */
/*==============================================================*/

create table ai_model_config
(
    id                    bigint not null,
    create_time           timestamp(6) with time zone not null default current_timestamp,
    create_by             bigint,
    update_time           timestamp(6) with time zone not null default current_timestamp,
    update_by             bigint,
    data_status           integer not null default 1,
    tenant_id             bigint not null,
    del_flag              smallint not null default 0,
    version               bigint not null default 0,
    connection_id         bigint not null,
    model_name            varchar(100) not null,
    model_type            varchar(32) not null,
    model_code            varchar(200) not null,
    embedding_dimensions  integer,
    capabilities          jsonb not null default '{}'::jsonb,
    default_options       jsonb not null default '{}'::jsonb,
    remark                varchar(500),
    primary key (id),
    -- 供知识库同时校验模型租户、标识和实际向量维度。
    constraint uk_ai_model_config_tenant_dimensions unique (tenant_id, id, embedding_dimensions),
    constraint fk_ai_model_config_tenant_connection
        foreign key (tenant_id, connection_id)
            references ai_model_connection (tenant_id, id),
    constraint ck_ai_model_config_status check (data_status in (0, 1)),
    constraint ck_ai_model_config_deleted check (del_flag in (0, 1)),
    constraint ck_ai_model_config_version check (version >= 0),
    constraint ck_ai_model_config_name check
        (model_name = btrim(model_name) and model_name <> ''),
    constraint ck_ai_model_config_code check
        (model_code = btrim(model_code) and model_code <> ''),
    constraint ck_ai_model_config_type check
        (model_type in ('chat', 'embedding', 'rerank', 'image', 'speech_to_text', 'text_to_speech')),
    -- 基础阶段允许向量模型未实测维度；其他类型不得混入维度参数。
    constraint ck_ai_model_config_dimensions check
        ((model_type = 'embedding' and (embedding_dimensions is null or embedding_dimensions > 0))
         or (model_type <> 'embedding' and embedding_dimensions is null)),
    constraint ck_ai_model_config_capabilities check (jsonb_typeof(capabilities) = 'object'),
    constraint ck_ai_model_config_options check (jsonb_typeof(default_options) = 'object')
);

comment on table ai_model_config is '租户模型配置；每条记录表示一种主能力，不保存模型权重或向量数据';
comment on column ai_model_config.id is '应用分配的全局主键，供知识库或 Agent 绑定模型配置';
comment on column ai_model_config.create_time is '创建时间点，Java 使用 Instant';
comment on column ai_model_config.create_by is '创建用户标识';
comment on column ai_model_config.update_time is '修改时间点，由应用在每次修改时更新';
comment on column ai_model_config.update_by is '修改用户标识';
comment on column ai_model_config.data_status is '启停状态：0禁用、1启用，对应 BasicConstant.DataStatus';
comment on column ai_model_config.tenant_id is '所属租户标识，与所引用连接的租户一致';
comment on column ai_model_config.del_flag is '逻辑删除标识：0未删除、1已删除';
comment on column ai_model_config.version is '乐观锁及模型配置版本；每次修改递增，与连接版本共同标识缓存实例';
comment on column ai_model_config.connection_id is '所属模型服务连接标识，通过带租户的复合外键防止跨租户引用';
comment on column ai_model_config.model_name is '页面显示名称，同租户同类型的未删除记录内唯一';
comment on column ai_model_config.model_type is '主能力：chat聊天、embedding向量、rerank重排序、image图片生成、speech_to_text语音识别、text_to_speech语音合成';
comment on column ai_model_config.model_code is '提供商要求的真实模型标识，例如 qwen-plus 或 qwen3-embedding:0.6b';
comment on column ai_model_config.embedding_dimensions is '仅向量模型使用的后端实测输出维度；基础阶段允许为空，页面不可编辑，当前不表示已验证';
comment on column ai_model_config.capabilities is '类型相关能力标记 JSON 对象，例如聊天模型的 toolCalling、visionInput、streaming；不是类型注册或可执行配置';
comment on column ai_model_config.default_options is '类型和提供商允许的默认调用参数 JSON 对象；禁止写入密钥、地址、模型标识或覆盖向量维度';
comment on column ai_model_config.remark is '模型用途说明，不允许填写敏感凭证';

create unique index uk_ai_model_config_tenant_type_name
    on ai_model_config (tenant_id, model_type, model_name)
    where del_flag = 0;

-- 页面先选择模型分类，再按创建时间倒序分页；启停状态作为附加筛选字段。
create index idx_ai_model_config_tenant_type_created
    on ai_model_config (tenant_id, model_type, create_time desc, id desc)
    include (data_status)
    where del_flag = 0;

-- 连接下模型查询、JOIN 及外键检查共用此索引，不仅覆盖未删除记录。
create index idx_ai_model_config_tenant_connection
    on ai_model_config (tenant_id, connection_id);

/*==============================================================*/
/* table: ai_knowledge_folder */
/*==============================================================*/
create table ai_knowledge_folder
(
    id                          bigint not null,
    create_time                 timestamp(6) with time zone not null default current_timestamp,
    create_by                   bigint,
    update_time                 timestamp(6) with time zone not null default current_timestamp,
    update_by                   bigint,
    data_status                 integer not null default 1,
    tenant_id                   bigint not null,
    del_flag                    smallint not null default 0,
    version                     bigint not null default 0,
    parent_id                   bigint,
    folder_name                 varchar(100) not null,
    description                 varchar(500),
    sort_order                  integer not null default 0,
    primary key (id),
    constraint ck_ai_knowledge_folder_status check (data_status in (0, 1)),
    constraint ck_ai_knowledge_folder_deleted check (del_flag in (0, 1)),
    constraint ck_ai_knowledge_folder_version check (version >= 0),
    constraint ck_ai_knowledge_folder_name check (folder_name = btrim(folder_name) and folder_name <> ''),
    constraint ck_ai_knowledge_folder_sort check (sort_order >= 0),
    constraint ck_ai_knowledge_folder_parent check (parent_id is null or parent_id <> id),
    constraint uk_ai_knowledge_folder_scope unique (tenant_id, id),
    constraint fk_ai_knowledge_folder_parent foreign key (tenant_id, parent_id)
        references ai_knowledge_folder (tenant_id, id)
);

comment on table ai_knowledge_folder is '知识库分类目录树；目录下创建知识库，文档归属知识库';
comment on column ai_knowledge_folder.id is '框架雪花算法分配的全局业务主键，不使用数据库自增';
comment on column ai_knowledge_folder.create_time is '创建时间点，Java 使用 Instant';
comment on column ai_knowledge_folder.create_by is '创建用户标识，仅用于审计，不作为目录数据权限过滤条件';
comment on column ai_knowledge_folder.update_time is '修改时间点，由应用在每次修改时更新';
comment on column ai_knowledge_folder.update_by is '修改用户标识';
comment on column ai_knowledge_folder.data_status is '启停状态：0禁用、1启用，对应 BasicConstant.DataStatus';
comment on column ai_knowledge_folder.tenant_id is '所属租户标识，从后端可信租户上下文取得';
comment on column ai_knowledge_folder.del_flag is '逻辑删除标识：0未删除、1已删除';
comment on column ai_knowledge_folder.version is '非负乐观锁版本，由应用在每次修改时递增';
comment on column ai_knowledge_folder.parent_id is '父目录标识，根目录为空；禁止自身及祖先循环，跨层循环由业务事务校验';
comment on column ai_knowledge_folder.folder_name is '目录名称，去除首尾空格，同租户同父目录的未删除记录内唯一';
comment on column ai_knowledge_folder.description is '目录描述，选填，最多500字';
comment on column ai_knowledge_folder.sort_order is '同级目录的非负排序值，升序排列，相同时按主键排序';

-- PostgreSQL 16 使用 NULLS NOT DISTINCT，根目录 parent_id 为空时也禁止同级重名。
create unique index uk_ai_knowledge_folder_name on ai_knowledge_folder
    (tenant_id, parent_id, folder_name) nulls not distinct where del_flag = 0;
-- 不限定逻辑删除，覆盖父目录外键检查；同时支持同级目录排序及递归树查询。
create index idx_ai_knowledge_folder_parent on ai_knowledge_folder
    (tenant_id, parent_id, sort_order, id);

/*==============================================================*/
/* table: ai_knowledge_base */
/*==============================================================*/
create table ai_knowledge_base
(
    id                          bigint not null,
    create_time                 timestamp(6) with time zone not null default current_timestamp,
    create_by                   bigint,
    update_time                 timestamp(6) with time zone not null default current_timestamp,
    update_by                   bigint,
    data_status                 integer not null default 1,
    tenant_id                   bigint not null,
    org_id                      bigint not null,
    folder_id                   bigint not null,
    is_public                   boolean not null default false,
    del_flag                    smallint not null default 0,
    version                     bigint not null default 0,
    knowledge_name              varchar(100) not null,
    description                 varchar(1000),
    retrieval_mode              varchar(16) not null default 'keyword',
    embedding_model_config_id   bigint,
    embedding_dimensions        integer,
    chunk_max_tokens            integer not null,
    chunk_overlap_tokens        integer not null,
    keyword_top_k               integer not null,
    vector_top_k                integer not null,
    retrieval_top_k             integer not null,
    similarity_threshold        double precision,
    primary key (id),
    constraint ck_ai_knowledge_base_status check (data_status in (0, 1)),
    constraint ck_ai_knowledge_base_deleted check (del_flag in (0, 1)),
    constraint ck_ai_knowledge_base_version check (version >= 0),
    constraint uk_ai_knowledge_base_tenant unique (tenant_id, id),
    constraint uk_ai_knowledge_base_vector_binding unique (tenant_id, id, embedding_model_config_id, embedding_dimensions),
    constraint fk_ai_knowledge_base_folder foreign key (tenant_id, folder_id)
        references ai_knowledge_folder (tenant_id, id),
    constraint fk_ai_knowledge_base_model foreign key (tenant_id, embedding_model_config_id, embedding_dimensions)
        references ai_model_config (tenant_id, id, embedding_dimensions),
    constraint ck_ai_knowledge_base_name check (knowledge_name = btrim(knowledge_name) and knowledge_name <> ''),
    constraint ck_ai_knowledge_base_mode check (retrieval_mode in ('keyword', 'vector', 'hybrid')),
    constraint ck_ai_knowledge_base_embedding check (
        (embedding_model_config_id is null and embedding_dimensions is null)
        or (embedding_model_config_id is not null and embedding_dimensions is not null
            and embedding_dimensions between 768 and 1536)),
    constraint ck_ai_knowledge_base_chunk_size check (chunk_max_tokens > 0 and chunk_overlap_tokens >= 0 and chunk_overlap_tokens < chunk_max_tokens),
    constraint ck_ai_knowledge_base_top_k check (keyword_top_k > 0 and vector_top_k > 0 and retrieval_top_k > 0
        and case retrieval_mode
            when 'keyword' then retrieval_top_k <= keyword_top_k
            when 'vector' then retrieval_top_k <= vector_top_k
            else retrieval_top_k::bigint <= keyword_top_k::bigint + vector_top_k::bigint end),
    constraint ck_ai_knowledge_base_threshold check (similarity_threshold is null or similarity_threshold between 0 and 1)
);

comment on table ai_knowledge_base is '租户知识库；统一管理全文、向量及混合检索策略';
comment on column ai_knowledge_base.id is '应用分配的全局业务主键，沿用 BaseEntity 的 ASSIGN_ID';
comment on column ai_knowledge_base.create_time is '创建时间点，Java 使用 Instant';
comment on column ai_knowledge_base.create_by is '创建用户标识';
comment on column ai_knowledge_base.update_time is '修改时间点，由应用在每次修改时更新';
comment on column ai_knowledge_base.update_by is '修改用户标识';
comment on column ai_knowledge_base.data_status is '启停状态：0禁用、1启用，对应 BasicConstant.DataStatus';
comment on column ai_knowledge_base.tenant_id is '所属租户标识，从后端可信租户上下文取得';
comment on column ai_knowledge_base.del_flag is '逻辑删除标识：0未删除、1已删除';
comment on column ai_knowledge_base.version is '非负乐观锁版本，由应用在每次修改时递增';
comment on column ai_knowledge_base.knowledge_name is '知识库显示名称，同租户未删除记录内唯一';
comment on column ai_knowledge_base.description is '知识库用途说明，不保存凭证';
comment on column ai_knowledge_base.retrieval_mode is '检索模式草稿：keyword全文、vector向量、hybrid混合；配置模式不表示检索能力已启用';
comment on column ai_knowledge_base.embedding_model_config_id is '本租户Embedding模型配置标识；基础阶段允许未绑定，当前页面接口不开放绑定及模型验证';
comment on column ai_knowledge_base.embedding_dimensions is '绑定模型的输出维度，范围768～1536；启用检索前必须实测核验；与模型绑定同时有值或同时为空，不设默认维度';
comment on column ai_knowledge_base.chunk_max_tokens is '分片目标 Token 上限，必须大于0，并在业务层校验模型输入限制';
comment on column ai_knowledge_base.chunk_overlap_tokens is '相邻分片重叠 Token 数，非负且小于分片上限';
comment on column ai_knowledge_base.keyword_top_k is '全文分支最大候选条数，正整数；非最终返回条数';
comment on column ai_knowledge_base.vector_top_k is '向量分支最大候选条数，正整数；非最终返回条数';
comment on column ai_knowledge_base.retrieval_top_k is '最终返回条数，正整数且不超过当前模式的候选容量';
comment on column ai_knowledge_base.similarity_threshold is '仅用于向量分支的0～1相似度阈值；为空时不额外限制得分，不作用于全文或RRF得分';

create unique index uk_ai_knowledge_base_name on ai_knowledge_base (tenant_id, folder_id, knowledge_name) where del_flag = 0;
create index idx_ai_knowledge_base_created on ai_knowledge_base (tenant_id, create_time desc, id desc) where del_flag = 0;
-- 包含已删除引用，支持模型外键检查及模型关联知识库查询。
create index idx_ai_knowledge_base_model on ai_knowledge_base (tenant_id, embedding_model_config_id, embedding_dimensions);

comment on column ai_knowledge_base.org_id is '知识库管理组织，公开读取不授予写权限，文档统一继承该管理范围';
comment on column ai_knowledge_base.folder_id is '必填分类目录，与知识库属于同一租户；移动知识库不改变文档归属';
comment on column ai_knowledge_base.is_public is '是否租户内公开；非管理用户读取文档还须文档自身公开';
-- 目录外键与目录下知识库列表共用此索引；不限定逻辑删除，支持外键检查。
create index idx_ai_knowledge_base_folder on ai_knowledge_base (folder_id, create_time desc, id desc);
create index idx_ai_knowledge_base_public on ai_knowledge_base (tenant_id, create_time desc, id desc)
    where del_flag = 0 and is_public;
create index idx_ai_knowledge_base_org on ai_knowledge_base (tenant_id, org_id, create_time desc, id desc)
    where del_flag = 0;
create index idx_ai_knowledge_base_creator on ai_knowledge_base (tenant_id, create_by, create_time desc, id desc)
    where del_flag = 0;

/*==============================================================*/
/* table: ai_knowledge_document */
/*==============================================================*/
create table ai_knowledge_document
(
    id                          bigint not null,
    create_time                 timestamp(6) with time zone not null default current_timestamp,
    create_by                   bigint,
    update_time                 timestamp(6) with time zone not null default current_timestamp,
    update_by                   bigint,
    data_status                 integer not null default 1,
    tenant_id                   bigint not null,
    del_flag                    smallint not null default 0,
    version                     bigint not null default 0,
    knowledge_base_id           bigint not null,
    is_public                   boolean not null default false,
    document_name               varchar(255) not null,
    original_filename           varchar(255) not null,
    storage_provider            varchar(32) not null default 'minio',
    storage_bucket              varchar(255) not null,
    storage_object_key          text not null,
    content_type                varchar(255) not null,
    file_size                   bigint not null,
    file_sha256                 varchar(64) not null,
    processing_status           varchar(32) not null default 'uploaded',
    processing_version          bigint not null default 0,
    processing_owner            uuid,
    processing_lease_until      timestamp(6) with time zone,
    processing_started_time     timestamp(6) with time zone,
    parser_version              varchar(64),
    splitter_version            varchar(64),
    chunk_max_tokens            integer,
    chunk_overlap_tokens        integer,
    error_message               varchar(1000),
    processed_time              timestamp(6) with time zone,
    primary key (id),
    constraint ck_ai_knowledge_document_status check (data_status in (0, 1)),
    constraint ck_ai_knowledge_document_deleted check (del_flag in (0, 1)),
    constraint ck_ai_knowledge_document_version check (version >= 0),
    constraint uk_ai_knowledge_document_scope unique (tenant_id, knowledge_base_id, id),
    constraint fk_ai_knowledge_document_base foreign key (tenant_id, knowledge_base_id) references ai_knowledge_base (tenant_id, id),
    constraint ck_ai_knowledge_document_name check (btrim(document_name) <> '' and btrim(original_filename) <> ''),
    constraint ck_ai_knowledge_document_storage check (storage_provider = 'minio' and btrim(storage_bucket) <> '' and btrim(storage_object_key) <> ''),
    constraint ck_ai_knowledge_document_file check (file_size > 0 and btrim(content_type) <> '' and file_sha256 ~ '^[0-9a-f]{64}$'),
    constraint ck_ai_knowledge_document_processing check (processing_status in ('uploaded', 'pending', 'parsing', 'preview', 'embedding', 'ready', 'failed') and processing_version >= 0),
    constraint ck_ai_knowledge_document_lease check (
        (processing_owner is null and processing_lease_until is null and processing_status not in ('parsing', 'embedding'))
        or (processing_owner is not null and processing_lease_until is not null and processing_started_time is not null
            and processing_lease_until > processing_started_time and processing_status in ('parsing', 'embedding'))),
    constraint ck_ai_knowledge_document_chunk_config check (
        (chunk_max_tokens is null and chunk_overlap_tokens is null and processing_status in ('uploaded', 'pending', 'parsing', 'failed'))
        or (chunk_max_tokens is not null and chunk_overlap_tokens is not null and chunk_max_tokens > 0
            and chunk_overlap_tokens >= 0 and chunk_overlap_tokens < chunk_max_tokens))
);

comment on table ai_knowledge_document is '知识库原始文件及当前处理轮次；文件内容由框架对象存储管理';
comment on column ai_knowledge_document.id is '应用分配的全局业务主键，沿用 BaseEntity 的 ASSIGN_ID';
comment on column ai_knowledge_document.create_time is '创建时间点，Java 使用 Instant';
comment on column ai_knowledge_document.create_by is '创建用户标识，同时作为框架本人数据权限的匹配字段';
comment on column ai_knowledge_document.update_time is '修改时间点，由应用在每次修改时更新';
comment on column ai_knowledge_document.update_by is '修改用户标识';
comment on column ai_knowledge_document.data_status is '启停状态：0禁用、1启用，对应 BasicConstant.DataStatus';
comment on column ai_knowledge_document.tenant_id is '所属租户标识，从后端可信租户上下文取得';
comment on column ai_knowledge_document.del_flag is '逻辑删除标识：0未删除、1已删除';
comment on column ai_knowledge_document.version is '非负乐观锁版本，由应用在每次修改时递增';
comment on column ai_knowledge_document.knowledge_base_id is '所属知识库标识，必须属于同一租户';
comment on column ai_knowledge_document.is_public is '是否租户内公开读取，默认false；非公开仅知识库管理范围可读；非管理用户须同时满足知识库公开和文档公开';
comment on column ai_knowledge_document.document_name is '页面显示文档名称，可与原文件名不同';
comment on column ai_knowledge_document.original_filename is '上传时的原始文件名，仅展示，不用于拼接本地路径';
comment on column ai_knowledge_document.storage_provider is '对象存储实现，第一版仅使用minio，由后端选择';
comment on column ai_knowledge_document.storage_bucket is '原始文件所在存储桶';
comment on column ai_knowledge_document.storage_object_key is '后端生成并限定租户边界的原始文件对象键，不保存签名URL';
comment on column ai_knowledge_document.content_type is '后端按文件名推断的MIME类型，未知类型为application/octet-stream，不表示已校验文件格式';
comment on column ai_knowledge_document.file_size is '原始文件字节数，必须大于0';
comment on column ai_knowledge_document.file_sha256 is '文件内容SHA-256小写十六进制摘要，不设置跨租户唯一约束';
comment on column ai_knowledge_document.processing_status is '处理状态：uploaded已上传未解析、pending待处理、parsing解析中、preview待确认分片、embedding向量化中、ready可检索、failed失败';
comment on column ai_knowledge_document.processing_version is '非负处理轮次，每次重新处理递增，异步任务必须携带并比较';
comment on column ai_knowledge_document.processing_owner is '当前处理任务的唯一租约标识；与租约到期时间同时有值或同时为空';
comment on column ai_knowledge_document.processing_lease_until is '当前任务租约到期时间；到期后通过条件更新抢占，旧任务禁止发布';
comment on column ai_knowledge_document.processing_started_time is '本轮处理开始时间点，不替代租约到期条件';
comment on column ai_knowledge_document.parser_version is '本轮实际解析器及规则版本，解析完成时由后端填写';
comment on column ai_knowledge_document.splitter_version is '本轮实际分词器及分片规则版本，分片完成时由后端填写';
comment on column ai_knowledge_document.chunk_max_tokens is '本轮实际使用的分片Token上限快照，分片后必填';
comment on column ai_knowledge_document.chunk_overlap_tokens is '本轮实际使用的重叠Token数快照，分片后必填';
comment on column ai_knowledge_document.error_message is '脱敏处理错误摘要，禁止保存密钥或完整模型请求';
comment on column ai_knowledge_document.processed_time is '本轮处理成功或失败的结束时间点';

create index idx_ai_knowledge_document_created on ai_knowledge_document
    (tenant_id, knowledge_base_id, create_time desc, id desc) where del_flag = 0;
create index idx_ai_knowledge_document_public on ai_knowledge_document
    (tenant_id, knowledge_base_id, create_time desc, id desc) where del_flag = 0 and data_status = 1 and is_public;
-- 复合唯一键已以租户、知识库开头，覆盖知识库外键检查，无需重复建立同前缀索引。
create index idx_ai_knowledge_document_expired on ai_knowledge_document
    (processing_lease_until, tenant_id, id)
    where del_flag = 0 and processing_status in ('parsing', 'embedding') and processing_owner is not null;

-- 原件清理队列的引用检查必须有索引，避免每批任务扫描所有文件。
create index idx_ai_knowledge_document_object on ai_knowledge_document
    (tenant_id, storage_bucket, storage_object_key) where del_flag = 0;

/*==============================================================*/
/* table: ai_knowledge_chunk */
/*==============================================================*/
create table ai_knowledge_chunk
(
    id                          bigint not null,
    create_time                 timestamp(6) with time zone not null default current_timestamp,
    create_by                   bigint,
    update_time                 timestamp(6) with time zone not null default current_timestamp,
    update_by                   bigint,
    data_status                 integer not null default 1,
    tenant_id                   bigint not null,
    del_flag                    smallint not null default 0,
    version                     bigint not null default 0,
    knowledge_base_id           bigint not null,
    document_id                 bigint not null,
    processing_version          bigint not null,
    chunk_index                 integer not null,
    title                       text not null default '',
    content                     text not null,
    search_vector               tsvector generated always as (public.ai_search_vector(title, content)) stored,
    token_count                 integer not null,
    source_location             jsonb not null default '{}'::jsonb,
    vector_id                   bigint not null,
    embedding_status            varchar(16) not null default 'pending',
    error_message               varchar(1000),
    embedded_time               timestamp(6) with time zone,
    primary key (id),
    constraint ck_ai_knowledge_chunk_status check (data_status in (0, 1)),
    constraint ck_ai_knowledge_chunk_deleted check (del_flag in (0, 1)),
    constraint ck_ai_knowledge_chunk_version check (version >= 0),
    constraint uk_ai_knowledge_chunk_order unique (tenant_id, knowledge_base_id, document_id, processing_version, chunk_index),
    constraint uk_ai_knowledge_chunk_vector_id unique (vector_id),
    constraint uk_ai_knowledge_chunk_vector_scope unique (tenant_id, knowledge_base_id, document_id, id, processing_version, vector_id),
    constraint fk_ai_knowledge_chunk_document foreign key (tenant_id, knowledge_base_id, document_id)
        references ai_knowledge_document (tenant_id, knowledge_base_id, id),
    constraint ck_ai_knowledge_chunk_content check (btrim(content) <> '' and token_count > 0),
    constraint ck_ai_knowledge_chunk_position check (processing_version >= 0 and chunk_index >= 0),
    constraint ck_ai_knowledge_chunk_location check (jsonb_typeof(source_location) = 'object'),
    constraint ck_ai_knowledge_chunk_embedding check (embedding_status in ('not_required', 'pending', 'ready', 'failed') and (embedding_status <> 'ready' or embedded_time is not null))
);

comment on table ai_knowledge_chunk is '知识库分片权威正文及中英文全文索引；向量按维度另表保存';
comment on column ai_knowledge_chunk.id is '应用分配的全局业务主键，沿用 BaseEntity 的 ASSIGN_ID';
comment on column ai_knowledge_chunk.create_time is '创建时间点，Java 使用 Instant';
comment on column ai_knowledge_chunk.create_by is '创建用户标识';
comment on column ai_knowledge_chunk.update_time is '修改时间点，由应用在每次修改时更新';
comment on column ai_knowledge_chunk.update_by is '修改用户标识';
comment on column ai_knowledge_chunk.data_status is '启停状态：0禁用、1启用，对应 BasicConstant.DataStatus';
comment on column ai_knowledge_chunk.tenant_id is '所属租户标识，从后端可信租户上下文取得';
comment on column ai_knowledge_chunk.del_flag is '逻辑删除标识：0未删除、1已删除';
comment on column ai_knowledge_chunk.version is '非负乐观锁版本，由应用在每次修改时递增';
comment on column ai_knowledge_chunk.knowledge_base_id is '所属知识库，必须与所属文档一致';
comment on column ai_knowledge_chunk.document_id is '所属文档标识，通过带租户、知识库的复合外键约束';
comment on column ai_knowledge_chunk.processing_version is '生成本分片的文档处理轮次，非负；发布和查询时还需核验文档当前轮次';
comment on column ai_knowledge_chunk.chunk_index is '本轮文档内的分片顺序，从0开始';
comment on column ai_knowledge_chunk.title is '章节标题，无章节标题时由后端使用文档显示名称，全文权重高于正文';
comment on column ai_knowledge_chunk.content is '可预览、编辑及重新生成向量的分片正文，不允许空白正文';
comment on column ai_knowledge_chunk.search_vector is '中英文全文词项生成列，标题或正文更新时同步重算，不由客户端赋值';
comment on column ai_knowledge_chunk.token_count is '实际分词器计算的Token数量，必须大于0';
comment on column ai_knowledge_chunk.source_location is '来源位置JSON对象，可记录页码、章节路径，不保存可执行参数';
comment on column ai_knowledge_chunk.vector_id is '框架雪花算法预分配的检索ID，作为Spring AI Document.id及对应向量主键，全局唯一，不使用数据库自增';
comment on column ai_knowledge_chunk.embedding_status is '向量状态：not_required无需向量、pending待处理、ready已完成、failed失败；纯全文创建时由后端选择not_required';
comment on column ai_knowledge_chunk.error_message is '当前分片向量化失败的脱敏摘要';
comment on column ai_knowledge_chunk.embedded_time is '当前正文成功写入向量的时间点，ready时必填';

-- 全文查询必须同时限制启停、删除及文档当前发布轮次；索引本身不承担租户授权。
create index idx_ai_knowledge_chunk_fulltext on ai_knowledge_chunk using gin (search_vector)
    where del_flag = 0 and data_status = 1;
-- 两个复合唯一键均覆盖文档外键前缀以及按文档、轮次读取，无需再建重复B-tree。

/*==============================================================*/
/* ai_vector_768：固定768维，Spring AI仍只写入四个基础字段。 */
/*==============================================================*/
create table ai_vector_768
(
    id bigint not null primary key,
    content text not null,
    metadata jsonb not null,
    embedding vector(768) not null,
    tenant_id bigint generated always as ((metadata->>'tenant_id')::bigint) stored not null,
    knowledge_base_id bigint generated always as ((metadata->>'knowledge_base_id')::bigint) stored not null,
    document_id bigint generated always as ((metadata->>'document_id')::bigint) stored not null,
    chunk_id bigint generated always as ((metadata->>'chunk_id')::bigint) stored not null,
    model_config_id bigint generated always as ((metadata->>'model_config_id')::bigint) stored not null,
    processing_version bigint generated always as ((metadata->>'processing_version')::bigint) stored not null,
    embedding_dimensions integer generated always as (768) stored not null,
    constraint fk_ai_vector_768_chunk foreign key
        (tenant_id, knowledge_base_id, document_id, chunk_id, processing_version, id)
        references ai_knowledge_chunk
        (tenant_id, knowledge_base_id, document_id, id, processing_version, vector_id),
    constraint fk_ai_vector_768_binding foreign key
        (tenant_id, knowledge_base_id, model_config_id, embedding_dimensions)
        references ai_knowledge_base
        (tenant_id, id, embedding_model_config_id, embedding_dimensions),
    constraint ck_ai_vector_768_content check (btrim(content) <> ''),
    constraint ck_ai_vector_768_metadata check (
        jsonb_typeof(metadata) = 'object'
        and metadata ? 'tenant_id' and jsonb_typeof(metadata->'tenant_id') = 'string'
        and (metadata->>'tenant_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'knowledge_base_id' and jsonb_typeof(metadata->'knowledge_base_id') = 'string'
        and (metadata->>'knowledge_base_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'document_id' and jsonb_typeof(metadata->'document_id') = 'string'
        and (metadata->>'document_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'chunk_id' and jsonb_typeof(metadata->'chunk_id') = 'string'
        and (metadata->>'chunk_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'model_config_id' and jsonb_typeof(metadata->'model_config_id') = 'string'
        and (metadata->>'model_config_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'processing_version' and jsonb_typeof(metadata->'processing_version') = 'string'
        and (metadata->>'processing_version') ~ '^(0|[1-9][0-9]*)$'
    )
);

comment on table ai_vector_768 is '768维检索投影；包含自动生成的归属字段以约束Spring AI四字段写入';
comment on column ai_vector_768.id is '等于业务分片vector_id的bigint雪花ID，由框架分配，不使用数据库自增或默认值';
comment on column ai_vector_768.content is '已向量化正文的检索副本，权威正文仍在分片表';
comment on column ai_vector_768.metadata is '后端构造的检索元数据；业务ID及处理版本必须保存为十进制字符串';
comment on column ai_vector_768.embedding is '恰好768维的非空向量，仅保存对应知识库绑定模型生成的结果';
comment on column ai_vector_768.tenant_id is '从metadata自动生成的租户标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_768.knowledge_base_id is '从metadata自动生成的知识库标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_768.document_id is '从metadata自动生成的文档标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_768.chunk_id is '从metadata自动生成的分片标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_768.model_config_id is '从metadata自动生成的模型配置标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_768.processing_version is '从metadata自动生成的处理轮次标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_768.embedding_dimensions is '由表定义固定生成的768维标识，用于校验知识库模型绑定';

create index idx_ai_vector_768_hnsw on ai_vector_768 using hnsw (embedding vector_cosine_ops);
-- 支持按租户、知识库、文档及轮次清理；完整键覆盖分片和模型绑定外键的关联前缀。
create index idx_ai_vector_768_scope on ai_vector_768
    (tenant_id, knowledge_base_id, document_id, chunk_id, processing_version, id);
create index idx_ai_vector_768_binding on ai_vector_768
    (tenant_id, knowledge_base_id, model_config_id, embedding_dimensions);
-- 原生PgVectorStore使用metadata JSON path过滤；真实召回SQL仍需校验业务发布状态。
create index idx_ai_vector_768_metadata on ai_vector_768 using gin (metadata jsonb_path_ops);

/*==============================================================*/
/* ai_vector_1024：固定1024维，Spring AI仍只写入四个基础字段。 */
/*==============================================================*/
create table ai_vector_1024
(
    id bigint not null primary key,
    content text not null,
    metadata jsonb not null,
    embedding vector(1024) not null,
    tenant_id bigint generated always as ((metadata->>'tenant_id')::bigint) stored not null,
    knowledge_base_id bigint generated always as ((metadata->>'knowledge_base_id')::bigint) stored not null,
    document_id bigint generated always as ((metadata->>'document_id')::bigint) stored not null,
    chunk_id bigint generated always as ((metadata->>'chunk_id')::bigint) stored not null,
    model_config_id bigint generated always as ((metadata->>'model_config_id')::bigint) stored not null,
    processing_version bigint generated always as ((metadata->>'processing_version')::bigint) stored not null,
    embedding_dimensions integer generated always as (1024) stored not null,
    constraint fk_ai_vector_1024_chunk foreign key
        (tenant_id, knowledge_base_id, document_id, chunk_id, processing_version, id)
        references ai_knowledge_chunk
        (tenant_id, knowledge_base_id, document_id, id, processing_version, vector_id),
    constraint fk_ai_vector_1024_binding foreign key
        (tenant_id, knowledge_base_id, model_config_id, embedding_dimensions)
        references ai_knowledge_base
        (tenant_id, id, embedding_model_config_id, embedding_dimensions),
    constraint ck_ai_vector_1024_content check (btrim(content) <> ''),
    constraint ck_ai_vector_1024_metadata check (
        jsonb_typeof(metadata) = 'object'
        and metadata ? 'tenant_id' and jsonb_typeof(metadata->'tenant_id') = 'string'
        and (metadata->>'tenant_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'knowledge_base_id' and jsonb_typeof(metadata->'knowledge_base_id') = 'string'
        and (metadata->>'knowledge_base_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'document_id' and jsonb_typeof(metadata->'document_id') = 'string'
        and (metadata->>'document_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'chunk_id' and jsonb_typeof(metadata->'chunk_id') = 'string'
        and (metadata->>'chunk_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'model_config_id' and jsonb_typeof(metadata->'model_config_id') = 'string'
        and (metadata->>'model_config_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'processing_version' and jsonb_typeof(metadata->'processing_version') = 'string'
        and (metadata->>'processing_version') ~ '^(0|[1-9][0-9]*)$'
    )
);

comment on table ai_vector_1024 is '1024维检索投影；包含自动生成的归属字段以约束Spring AI四字段写入';
comment on column ai_vector_1024.id is '等于业务分片vector_id的bigint雪花ID，由框架分配，不使用数据库自增或默认值';
comment on column ai_vector_1024.content is '已向量化正文的检索副本，权威正文仍在分片表';
comment on column ai_vector_1024.metadata is '后端构造的检索元数据；业务ID及处理版本必须保存为十进制字符串';
comment on column ai_vector_1024.embedding is '恰好1024维的非空向量，仅保存对应知识库绑定模型生成的结果';
comment on column ai_vector_1024.tenant_id is '从metadata自动生成的租户标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1024.knowledge_base_id is '从metadata自动生成的知识库标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1024.document_id is '从metadata自动生成的文档标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1024.chunk_id is '从metadata自动生成的分片标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1024.model_config_id is '从metadata自动生成的模型配置标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1024.processing_version is '从metadata自动生成的处理轮次标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1024.embedding_dimensions is '由表定义固定生成的1024维标识，用于校验知识库模型绑定';

create index idx_ai_vector_1024_hnsw on ai_vector_1024 using hnsw (embedding vector_cosine_ops);
-- 支持按租户、知识库、文档及轮次清理；完整键覆盖分片和模型绑定外键的关联前缀。
create index idx_ai_vector_1024_scope on ai_vector_1024
    (tenant_id, knowledge_base_id, document_id, chunk_id, processing_version, id);
create index idx_ai_vector_1024_binding on ai_vector_1024
    (tenant_id, knowledge_base_id, model_config_id, embedding_dimensions);
-- 原生PgVectorStore使用metadata JSON path过滤；真实召回SQL仍需校验业务发布状态。
create index idx_ai_vector_1024_metadata on ai_vector_1024 using gin (metadata jsonb_path_ops);

/*==============================================================*/
/* ai_vector_1536：固定1536维，Spring AI仍只写入四个基础字段。 */
/*==============================================================*/
create table ai_vector_1536
(
    id bigint not null primary key,
    content text not null,
    metadata jsonb not null,
    embedding vector(1536) not null,
    tenant_id bigint generated always as ((metadata->>'tenant_id')::bigint) stored not null,
    knowledge_base_id bigint generated always as ((metadata->>'knowledge_base_id')::bigint) stored not null,
    document_id bigint generated always as ((metadata->>'document_id')::bigint) stored not null,
    chunk_id bigint generated always as ((metadata->>'chunk_id')::bigint) stored not null,
    model_config_id bigint generated always as ((metadata->>'model_config_id')::bigint) stored not null,
    processing_version bigint generated always as ((metadata->>'processing_version')::bigint) stored not null,
    embedding_dimensions integer generated always as (1536) stored not null,
    constraint fk_ai_vector_1536_chunk foreign key
        (tenant_id, knowledge_base_id, document_id, chunk_id, processing_version, id)
        references ai_knowledge_chunk
        (tenant_id, knowledge_base_id, document_id, id, processing_version, vector_id),
    constraint fk_ai_vector_1536_binding foreign key
        (tenant_id, knowledge_base_id, model_config_id, embedding_dimensions)
        references ai_knowledge_base
        (tenant_id, id, embedding_model_config_id, embedding_dimensions),
    constraint ck_ai_vector_1536_content check (btrim(content) <> ''),
    constraint ck_ai_vector_1536_metadata check (
        jsonb_typeof(metadata) = 'object'
        and metadata ? 'tenant_id' and jsonb_typeof(metadata->'tenant_id') = 'string'
        and (metadata->>'tenant_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'knowledge_base_id' and jsonb_typeof(metadata->'knowledge_base_id') = 'string'
        and (metadata->>'knowledge_base_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'document_id' and jsonb_typeof(metadata->'document_id') = 'string'
        and (metadata->>'document_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'chunk_id' and jsonb_typeof(metadata->'chunk_id') = 'string'
        and (metadata->>'chunk_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'model_config_id' and jsonb_typeof(metadata->'model_config_id') = 'string'
        and (metadata->>'model_config_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'processing_version' and jsonb_typeof(metadata->'processing_version') = 'string'
        and (metadata->>'processing_version') ~ '^(0|[1-9][0-9]*)$'
    )
);

comment on table ai_vector_1536 is '1536维检索投影；包含自动生成的归属字段以约束Spring AI四字段写入';
comment on column ai_vector_1536.id is '等于业务分片vector_id的bigint雪花ID，由框架分配，不使用数据库自增或默认值';
comment on column ai_vector_1536.content is '已向量化正文的检索副本，权威正文仍在分片表';
comment on column ai_vector_1536.metadata is '后端构造的检索元数据；业务ID及处理版本必须保存为十进制字符串';
comment on column ai_vector_1536.embedding is '恰好1536维的非空向量，仅保存对应知识库绑定模型生成的结果';
comment on column ai_vector_1536.tenant_id is '从metadata自动生成的租户标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1536.knowledge_base_id is '从metadata自动生成的知识库标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1536.document_id is '从metadata自动生成的文档标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1536.chunk_id is '从metadata自动生成的分片标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1536.model_config_id is '从metadata自动生成的模型配置标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1536.processing_version is '从metadata自动生成的处理轮次标识，用于复合外键及索引，不由Spring AI直接写入';
comment on column ai_vector_1536.embedding_dimensions is '由表定义固定生成的1536维标识，用于校验知识库模型绑定';

create index idx_ai_vector_1536_hnsw on ai_vector_1536 using hnsw (embedding vector_cosine_ops);
-- 支持按租户、知识库、文档及轮次清理；完整键覆盖分片和模型绑定外键的关联前缀。
create index idx_ai_vector_1536_scope on ai_vector_1536
    (tenant_id, knowledge_base_id, document_id, chunk_id, processing_version, id);
create index idx_ai_vector_1536_binding on ai_vector_1536
    (tenant_id, knowledge_base_id, model_config_id, embedding_dimensions);
-- 原生PgVectorStore使用metadata JSON path过滤；真实召回SQL仍需校验业务发布状态。
create index idx_ai_vector_1536_metadata on ai_vector_1536 using gin (metadata jsonb_path_ops);

-- 上传会话引用知识库及文档，必须在被引用表创建完成后定义。
/* 文件传输会话独立于解析工作流；持久化支持幂等合并和过期清理。 */
create table ai_file_upload (
    id bigint primary key,
    create_time timestamp(6) with time zone not null default current_timestamp,
    create_by bigint,
    update_time timestamp(6) with time zone not null default current_timestamp,
    update_by bigint,
    data_status integer not null default 1 check (data_status in (0,1)),
    tenant_id bigint not null,
    del_flag smallint not null default 0 check (del_flag in (0,1)),
    version bigint not null default 0 check (version >= 0),
    knowledge_base_id bigint not null,
    replace_document_id bigint,
    replace_version bigint,
    original_filename varchar(255) not null,
    file_size bigint not null check (file_size > 0),
    bucket varchar(255) not null,
    object_key text not null,
    upload_id text,
    state varchar(20) not null check (state in ('initializing','uploading','completed','cancelled')),
    document_id bigint,
    part_size_bytes bigint not null check (part_size_bytes between 5242880 and 5368709120),
    part_count integer not null check (part_count between 1 and 10000),
    url_expiry_seconds integer not null check (url_expiry_seconds between 1 and 604800),
    concurrency integer not null check (concurrency >= 1),
    max_retries integer not null check (max_retries >= 0),
    expires_at timestamp(6) with time zone not null,
    constraint fk_ai_file_upload_base foreign key (tenant_id,knowledge_base_id) references ai_knowledge_base(tenant_id,id),
    constraint fk_ai_file_upload_replace foreign key (tenant_id,knowledge_base_id,replace_document_id) references ai_knowledge_document(tenant_id,knowledge_base_id,id),
    constraint fk_ai_file_upload_document foreign key (tenant_id,knowledge_base_id,document_id) references ai_knowledge_document(tenant_id,knowledge_base_id,id),
    constraint uk_ai_file_upload_object unique (tenant_id,bucket,object_key),
    constraint ck_ai_file_upload_replacement check ((replace_document_id is null and replace_version is null)
        or (replace_document_id is not null and replace_version is not null and replace_version >= 0)),
    constraint ck_ai_file_upload_completed check ((state='completed' and document_id is not null)
        or (state <> 'completed' and document_id is null)),
    constraint ck_ai_file_upload_parts check (part_count = ((file_size-1)/part_size_bytes)+1),
    constraint ck_ai_file_upload_remote check (state <> 'uploading' or nullif(btrim(upload_id),'') is not null)
);
create index idx_ai_file_upload_expired on ai_file_upload(expires_at,id) where state in ('initializing','uploading') and del_flag=0;
create index idx_ai_file_upload_replace on ai_file_upload(tenant_id,knowledge_base_id,replace_document_id);
create index idx_ai_file_upload_document on ai_file_upload(tenant_id,knowledge_base_id,document_id);

comment on table ai_file_upload is '文件分片传输持久化任务';
comment on column ai_file_upload.id is '框架雪花主键，不使用数据库自增';
comment on column ai_file_upload.create_time is '创建时间点，Java使用Instant';
comment on column ai_file_upload.create_by is '任务创建用户，仅原用户允许操作会话';
comment on column ai_file_upload.update_time is '更新时间点';
comment on column ai_file_upload.update_by is '更新用户标识';
comment on column ai_file_upload.data_status is '基础状态，0禁用1启用';
comment on column ai_file_upload.tenant_id is '可信租户标识';
comment on column ai_file_upload.del_flag is '逻辑删除，0未删除1已删除';
comment on column ai_file_upload.version is '非负乐观锁版本';
comment on column ai_file_upload.knowledge_base_id is '所属知识库';
comment on column ai_file_upload.replace_document_id is '可选替换文档，必须同租户同知识库';
comment on column ai_file_upload.replace_version is '替换文档预期版本，防止上传期间文件已改变';
comment on column ai_file_upload.original_filename is '原始文件名，不作为存储路径';
comment on column ai_file_upload.file_size is '声明字节数，合并后必须按实际流核验';
comment on column ai_file_upload.bucket is '后端存储桶，不返回页面';
comment on column ai_file_upload.object_key is '后端唯一对象键，包含租户及任务边界';
comment on column ai_file_upload.upload_id is '真实MinIO分片任务ID，不返回前端';
comment on column ai_file_upload.state is '传输状态：initializing初始化、uploading上传、completed完成、cancelled取消或过期';
comment on column ai_file_upload.document_id is '完成后文档ID，用于幂等确认';
comment on column ai_file_upload.part_size_bytes is '客户端提交的固定分片字节数，满足S3的5MiB至5GiB协议范围';
comment on column ai_file_upload.part_count is '固定片数，1至10000';
comment on column ai_file_upload.url_expiry_seconds is '客户端提交的分片签名有效秒数，S3协议允许1至604800';
comment on column ai_file_upload.concurrency is '客户端提交的组件队列并发快照，正整数';
comment on column ai_file_upload.max_retries is '客户端提交的每片额外重试次数快照，非负整数';
comment on column ai_file_upload.expires_at is '任务过期时间点，过期后不允许签发或合并';




/* 原件清理意图与文件数据库修改共同提交，存储失败不丢失重试任务。 */
create table ai_storage_cleanup (
    id bigint primary key,
    create_time timestamp(6) with time zone not null default current_timestamp,
    create_by bigint,
    update_time timestamp(6) with time zone not null default current_timestamp,
    update_by bigint,
    data_status integer not null default 1 check (data_status in (0,1)),
    tenant_id bigint not null,
    del_flag smallint not null default 0 check (del_flag in (0,1)),
    bucket varchar(255) not null,
    object_key text not null,
    upload_id text,
    abort_multipart boolean not null,
    next_attempt_at timestamp(6) with time zone not null default current_timestamp,
    attempts integer not null default 0 check (attempts >= 0)
);
create index idx_ai_storage_cleanup_due on ai_storage_cleanup(next_attempt_at,id);

comment on table ai_storage_cleanup is '存储清理事务队列，无公开HTTP接口';
comment on column ai_storage_cleanup.id is '框架雪花主键';
comment on column ai_storage_cleanup.create_time is '创建时间点';
comment on column ai_storage_cleanup.create_by is '发起用户，系统维护可为空';
comment on column ai_storage_cleanup.update_time is '更新时间点';
comment on column ai_storage_cleanup.update_by is '更新用户，系统维护可为空';
comment on column ai_storage_cleanup.data_status is '基础状态，0禁用1启用';
comment on column ai_storage_cleanup.tenant_id is '待清理对象所属可信租户';
comment on column ai_storage_cleanup.del_flag is '基础逻辑删除标记，完成任务直接物理移除';
comment on column ai_storage_cleanup.bucket is '待清理桶';
comment on column ai_storage_cleanup.object_key is '待清理唯一对象键，不存储签名URL';
comment on column ai_storage_cleanup.upload_id is '可选MinIO任务ID，缺失时按唯一对象键查找';
comment on column ai_storage_cleanup.abort_multipart is '是否同时中止未完成分片任务';
comment on column ai_storage_cleanup.next_attempt_at is '允许下次重试的时间点';
comment on column ai_storage_cleanup.attempts is '清理失败次数，失败保持任务并延迟重试';

commit;
