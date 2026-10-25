/*==============================================================*/
/* SaaS 模型、知识库目录及资料上传的初始结构。           */
/* 本脚本描述当前最终结构，由部署流程显式执行，不在应用启动时执行。 */
/*==============================================================*/

-- 数据库服务端须预先安装 zhparser 及其 SCWS 依赖；分片全文索引使用固定中文分词配置。
begin;
set local search_path = public, pg_catalog;

create extension if not exists zhparser with schema public;

-- 保留常用中文词、英文及数字词元；建索引和查询必须显式使用同一个配置。
create text search configuration public.etd_knowledge_zh (parser = public.zhparser);
alter text search configuration public.etd_knowledge_zh
    add mapping for n, v, a, i, e, l, j, x, m, t with pg_catalog.simple;
comment on text search configuration public.etd_knowledge_zh is '知识分片中文全文检索配置，基于zhparser分词并保留英文和数字词元';

-- 固定短词复合分词，保留复合词及其短词；避免不同连接设置导致建索引和查询的分词不一致。
create function public.etd_knowledge_to_tsvector(content text) returns tsvector
    language sql immutable strict
    set zhparser.multi_short = on
    set zhparser.multi_duality = off
    set zhparser.multi_zmain = off
    set zhparser.multi_zall = off
    as $$ select pg_catalog.to_tsvector('public.etd_knowledge_zh'::regconfig, content) $$;
comment on function public.etd_knowledge_to_tsvector(text) is '知识分片正文分词入口，固定启用短词复合，供全文GIN索引和查询使用';

create function public.etd_knowledge_to_tsquery(keyword text) returns tsquery
    language sql immutable strict
    set zhparser.multi_short = on
    set zhparser.multi_duality = off
    set zhparser.multi_zmain = off
    set zhparser.multi_zall = off
    as $$ select pg_catalog.plainto_tsquery('public.etd_knowledge_zh'::regconfig, keyword) $$;
comment on function public.etd_knowledge_to_tsquery(text) is '知识分片查询词分词入口，采用与正文相同的分词设置，不解释用户输入为tsquery语法';

create table ai_model
(
    id                       bigint not null,
    create_time              timestamp(6) with time zone not null default current_timestamp,
    create_by                bigint,
    update_time              timestamp(6) with time zone not null default current_timestamp,
    update_by                bigint,
    data_status              integer not null default 1,
    del_flag                 smallint not null default 0,
    version                  bigint not null default 0,
    provider_code            varchar(64) not null,
    model_type               varchar(32) not null,
    model_name               varchar(100) not null,
    model_code               varchar(200) not null,
    logo_path                varchar(1000),
    description              varchar(1000),
    api_key_url              varchar(1000),
    setup_guide_url          varchar(1000),
    base_url_help            varchar(1000),
    embedding                integer[],
    sort                     integer not null default 0,
    primary key (id),
    constraint ck_ai_model_version check (version >= 0),
    constraint ck_ai_model_status check (data_status in (0, 1)),
    constraint ck_ai_model_deleted check (del_flag in (0, 1)),
    constraint ck_ai_model_name check (model_name = btrim(model_name) and model_name <> ''),
    constraint ck_ai_model_code check (model_code = btrim(model_code) and model_code <> ''),
    constraint ck_ai_model_provider check (provider_code = btrim(provider_code) and provider_code <> ''),
    constraint ck_ai_model_type check (model_type in ('chat', 'embedding', 'rerank', 'image', 'speech_to_text', 'text_to_speech')),
    constraint ck_ai_model_embedding check (
        (model_type = 'embedding' and embedding is not null
            and cardinality(embedding) > 0)
        or (model_type <> 'embedding' and embedding is null)),
    constraint ck_ai_model_sort check (sort >= 0)
);

comment on table ai_model is '平台全局模型管理：平台管理员维护，所有租户共用；不保存租户连接及凭证';
comment on column ai_model.id is '主键ID，由应用分配雪花ID';
comment on column ai_model.create_time is '创建时间，保存带时区的绝对时间点';
comment on column ai_model.create_by is '创建用户ID';
comment on column ai_model.update_time is '最后修改时间，保存带时区的绝对时间点';
comment on column ai_model.update_by is '最后修改用户ID';
comment on column ai_model.data_status is '启停状态：0禁用、1启用';
comment on column ai_model.del_flag is '逻辑删除：0未删除、1已删除';
comment on column ai_model.version is '乐观锁版本，更新必须携带当前值';
comment on column ai_model.provider_code is '服务商字典ai_model_provider的值，与接入协议分开';
comment on column ai_model.model_type is '模型类型字典ai_model_type的值，必须有对应的后端能力支持';
comment on column ai_model.model_name is '平台模型展示名称';
comment on column ai_model.model_code is '调用模型接口使用的真实模型代码，由平台维护';
comment on column ai_model.logo_path is '模型Logo资源路径';
comment on column ai_model.description is '模型用途及特点介绍';
comment on column ai_model.api_key_url is '获取API Key的服务商页面地址，不保存密钥';
comment on column ai_model.setup_guide_url is '模型开通及协议接入指南地址';
comment on column ai_model.base_url_help is '实际服务地址的获取及填写说明，不能填入租户专属地址';
comment on column ai_model.embedding is '平台登记的模型支持维度集合，向量模型必填且为不重复正整数；非向量模型为空；资料实际维度须同时受该集合及系统存储维度约束';
comment on column ai_model.sort is '模型管理排序值，越小越靠前';

-- 一个服务商下的有效调用模型代码唯一；不同服务商可收录相同模型。
create unique index uk_ai_model_provider_code on ai_model (provider_code, model_code) where del_flag = 0;
-- 覆盖目录四种筛选组合：无筛选、仅服务商、仅类型及服务商加类型。
create index idx_ai_model_sort on ai_model (sort, id) where del_flag = 0;
create index idx_ai_model_provider_sort on ai_model (provider_code, sort, id) where del_flag = 0;
create index idx_ai_model_type_sort on ai_model (model_type, sort, id) where del_flag = 0;
create index idx_ai_model_provider_type_sort on ai_model (provider_code, model_type, sort, id)
    where del_flag = 0;

create table ai_model_connection
(
    id                       bigint not null,
    create_time              timestamp(6) with time zone not null default current_timestamp,
    create_by                bigint,
    update_time              timestamp(6) with time zone not null default current_timestamp,
    update_by                bigint,
    data_status              integer not null default 1,
    del_flag                 smallint not null default 0,
    tenant_id                bigint not null,
    version                  bigint not null default 0,
    model_id                 bigint not null,
    protocol                 varchar(32) not null,
    base_url                 varchar(1000) not null,
    auth_type                varchar(16) not null,
    api_key_ciphertext       text,
    credential_key_id        varchar(64),
    primary key (id),
    constraint ck_ai_model_connection_status check (data_status in (0, 1)),
    constraint ck_ai_model_connection_deleted check (del_flag in (0, 1)),
    constraint ck_ai_model_connection_version check (version >= 0),
    constraint ck_ai_model_connection_protocol check (protocol in ('dashscope', 'ollama', 'openai_compatible')),
    constraint ck_ai_model_connection_url check (base_url ~ '^https?://[^[:space:]]+$'),
    constraint ck_ai_model_connection_auth check ((auth_type = 'none' and api_key_ciphertext is null and credential_key_id is null) or (auth_type = 'api_key' and nullif(btrim(api_key_ciphertext), '') is not null and nullif(btrim(credential_key_id), '') is not null))
);

comment on table ai_model_connection is '租户模型连接器：同一租户的一个平台模型仅允许一个未删除连接器，保存协议、实际地址及认证凭证';
comment on column ai_model_connection.id is '主键ID，由应用分配雪花ID';
comment on column ai_model_connection.create_time is '创建时间，保存带时区的绝对时间点';
comment on column ai_model_connection.create_by is '创建用户ID';
comment on column ai_model_connection.update_time is '最后修改时间，保存带时区的绝对时间点';
comment on column ai_model_connection.update_by is '最后修改用户ID';
comment on column ai_model_connection.data_status is '启停状态：0禁用、1启用';
comment on column ai_model_connection.del_flag is '逻辑删除：0未删除、1已删除';
comment on column ai_model_connection.tenant_id is '所属租户，由可信请求上下文填充';
comment on column ai_model_connection.version is '乐观锁版本，更新必须携带当前版本';
comment on column ai_model_connection.model_id is '所选平台模型ID，创建后不允许更换';
comment on column ai_model_connection.protocol is '租户选择的接入协议，不是服务商编码；保存不表示调用已经验证';
comment on column ai_model_connection.base_url is '租户实际服务地址，不得含凭证、查询参数或片段';
comment on column ai_model_connection.auth_type is '认证方式：none无需认证、api_key密钥认证';
comment on column ai_model_connection.api_key_ciphertext is 'AES-GCM加密的API Key，不返回前端';
comment on column ai_model_connection.credential_key_id is '服务端加密密钥标识，实际主密钥由环境注入';

-- 停用连接仍占用模型绑定，逻辑删除后允许重新接入；不同租户独立绑定。
create index idx_ai_model_connection_tenant_created
    on ai_model_connection (tenant_id, create_time desc, id desc) include (data_status) where del_flag = 0;
create unique index uk_ai_model_connection_tenant_model
    on ai_model_connection (tenant_id, model_id) include (data_status) where del_flag = 0;
-- 覆盖平台跨租户的有效连接分组和启停统计；租户统计复用上方唯一索引。
create index idx_ai_model_connection_model on ai_model_connection (model_id)
    include (data_status) where del_flag = 0;

/*==============================================================*/
/* table: ai_knowledge_folder */
/*==============================================================*/
-- 知识库承接数据权限；分类与资料均属于该边界。
create table ai_knowledge_base
(
    id bigint not null primary key,
    create_time timestamp(6) with time zone not null default current_timestamp,
    create_by bigint,
    update_time timestamp(6) with time zone not null default current_timestamp,
    update_by bigint,
    tenant_id bigint not null,
    org_id bigint not null,
    del_flag smallint not null default 0,
    data_status integer not null default 1,
    version bigint not null default 0,
    name varchar(100) not null,
    description varchar(1000),
    is_public boolean not null default false,
    retrieval_mode varchar(16) not null default 'keyword',
    keyword_recall_size integer not null default 30,
    vector_recall_size integer not null default 30,
    top_k integer not null default 5,
    similarity_threshold double precision,
    context_window_size integer not null default 0,
    rerank_enabled boolean not null default false,
    rerank_connection_id bigint,
    rerank_threshold double precision,
    embedding_connection_id bigint,
    dimensions integer,
    active_connection_id bigint,
    active_dimensions integer,
    active_index_version bigint not null default 0,
    building_index_version bigint not null default 0,
    index_status varchar(16) not null default 'idle',
    index_total integer not null default 0,
    index_completed integer not null default 0,
    index_error varchar(1000),
    index_started_time timestamp(6) with time zone,
    index_lease_until timestamp(6) with time zone,
    constraint ck_ai_base_name check (btrim(name) <> ''),
    constraint ck_ai_base_status check (data_status in (0, 1) and del_flag in (0, 1)),
    constraint ck_ai_base_mode check (retrieval_mode in ('keyword', 'vector', 'hybrid')),
    constraint ck_ai_base_dimensions check (dimensions in (768, 1024, 1536) and active_dimensions in (768, 1024, 1536)),
    constraint ck_ai_base_embedding check ((embedding_connection_id is null) = (dimensions is null)),
    constraint ck_ai_base_active check ((active_connection_id is null) = (active_dimensions is null)
        and ((active_index_version = 0 and active_connection_id is null) or (active_index_version > 0 and active_connection_id is not null))),
    constraint ck_ai_base_rerank check ((not rerank_enabled and rerank_connection_id is null) or (rerank_enabled and rerank_connection_id is not null)),
    constraint ck_ai_base_counts check (keyword_recall_size between 1 and 200 and vector_recall_size between 1 and 200 and top_k between 1 and 100),
    constraint ck_ai_base_threshold check (similarity_threshold between -1 and 1),
    constraint ck_ai_base_context check (context_window_size between 0 and 3),
    constraint ck_ai_base_index check (index_status in ('idle', 'building', 'ready', 'failed') and index_total >= 0 and index_completed between 0 and index_total)
);
comment on table ai_knowledge_base is '知识库基本信息、检索配置及当前/待生效索引状态';
create unique index uk_ai_base_name on ai_knowledge_base (tenant_id, name) where del_flag = 0;
create index idx_ai_base_org on ai_knowledge_base (tenant_id, org_id, create_time desc, id desc) where del_flag = 0;
create index idx_ai_base_creator on ai_knowledge_base (tenant_id, create_by) where del_flag = 0;
create index idx_ai_base_public on ai_knowledge_base (tenant_id) where is_public = true and del_flag = 0;

comment on column ai_knowledge_base.version is '乐观锁版本，更新时必须回传。';
comment on column ai_knowledge_base.name is '知识库名称，租户内唯一。';
comment on column ai_knowledge_base.description is '知识库简介。';
comment on column ai_knowledge_base.is_public is '是否租户内共享，并作为新文档公开默认值。';
comment on column ai_knowledge_base.retrieval_mode is '检索模式：keyword、vector、hybrid。';
comment on column ai_knowledge_base.keyword_recall_size is '全文召回数量。';
comment on column ai_knowledge_base.vector_recall_size is '向量召回数量。';
comment on column ai_knowledge_base.top_k is '最终返回数量上限。';
comment on column ai_knowledge_base.similarity_threshold is '原始余弦相似度下限，空表示不过滤。';
comment on column ai_knowledge_base.context_window_size is '检索命中后的指针滑动上下文扩展窗口，0表示不扩展，1表示向前后各扩展1个相邻切片，上限为3。';
comment on column ai_knowledge_base.rerank_enabled is '是否启用重排。';
comment on column ai_knowledge_base.rerank_connection_id is '重排模型连接ID，关闭时为空。';
comment on column ai_knowledge_base.rerank_threshold is '重排分数下限，空表示不限制。';
comment on column ai_knowledge_base.embedding_connection_id is '待构建的向量模型连接ID。';
comment on column ai_knowledge_base.dimensions is '待构建维度，仅768、1024或1536。';
comment on column ai_knowledge_base.active_connection_id is '正式生效索引使用的模型连接ID。';
comment on column ai_knowledge_base.active_dimensions is '正式生效索引维度。';
comment on column ai_knowledge_base.active_index_version is '生效向量索引代次，0表示尚未初始化。';
comment on column ai_knowledge_base.building_index_version is '正在构建或最近失败的索引代次。';
comment on column ai_knowledge_base.index_status is 'idle、building、ready或failed。';
comment on column ai_knowledge_base.index_total is '本次需要处理的分片数。';
comment on column ai_knowledge_base.index_completed is '本次已完成分片数。';
comment on column ai_knowledge_base.index_error is '脱敏后的索引错误。';
comment on column ai_knowledge_base.index_started_time is '本次构建开始时间，用于超时恢复。';

create table ai_knowledge_folder
(
    id                          bigint not null,
    create_time                 timestamp(6) with time zone not null default current_timestamp,
    create_by                   bigint,
    update_time                 timestamp(6) with time zone not null default current_timestamp,
    update_by                   bigint,
    data_status                 integer not null default 1,
    tenant_id                   bigint not null,
    knowledge_base_id           bigint not null,
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
    constraint ck_ai_knowledge_folder_parent check (parent_id is null or parent_id <> id)
);

comment on table ai_knowledge_folder is '知识库目录：知识库内部分类树，资料直接归属于目录';
comment on column ai_knowledge_folder.id is '主键ID，由应用分配全局唯一的雪花ID，不使用数据库自增';
comment on column ai_knowledge_folder.create_time is '创建时间，默认当前时间；保存带时区的时间点';
comment on column ai_knowledge_folder.create_by is '目录创建用户ID，可为空；仅用于审计，不决定目录权限';
comment on column ai_knowledge_folder.update_time is '最后修改时间，新增时默认当前时间，后续修改由应用更新';
comment on column ai_knowledge_folder.update_by is '最后修改用户ID，可为空';
comment on column ai_knowledge_folder.data_status is '启停状态：0禁用、1启用，默认1';
comment on column ai_knowledge_folder.tenant_id is '所属租户ID，由后端租户上下文确定';
comment on column ai_knowledge_folder.del_flag is '逻辑删除标记：0未删除、1已删除，默认0';
comment on column ai_knowledge_folder.version is '乐观锁版本号，默认0且不能为负；用于检测并发修改，由应用维护递增';
comment on column ai_knowledge_folder.parent_id is '父目录ID，关联本表id；根目录为空，父子目录须属于同一知识库，不允许目录循环引用';
comment on column ai_knowledge_folder.folder_name is '目录名称，最多100字；不能有首尾空格，同知识库同父目录的未删除目录不能重名';
comment on column ai_knowledge_folder.description is '目录用途或分类说明，可为空，最多500字';
comment on column ai_knowledge_folder.sort_order is '同级目录排序值，默认0且不能为负；数值越小越靠前，相同时按ID排序';

-- 同一知识库同级目录唯一，根目录空父节点同样受约束。
create unique index uk_ai_knowledge_folder_name on ai_knowledge_folder
    (knowledge_base_id, parent_id, folder_name) nulls not distinct where del_flag = 0;
create index idx_ai_knowledge_folder_parent on ai_knowledge_folder
    (knowledge_base_id, parent_id, sort_order, id) where del_flag = 0;
comment on column ai_knowledge_folder.knowledge_base_id is '所属知识库ID，分类无独立数据权限';

/*==============================================================*/
/* table: ai_knowledge_document */
/*==============================================================*/
create table ai_knowledge_document
(
    knowledge_base_id bigint not null,
    processing_version bigint not null default 0,
    id                          bigint not null,
    create_time                 timestamp(6) with time zone not null default current_timestamp,
    create_by                   bigint,
    update_time                 timestamp(6) with time zone not null default current_timestamp,
    update_by                   bigint,
    data_status                 integer not null default 0,
    tenant_id                   bigint not null,
    org_id                      bigint not null,
    del_flag                    smallint not null default 0,
    version                     bigint not null default 0,
    is_public                   boolean not null default false,
    folder_id                   bigint not null,
    description                 varchar(1000),
    upload_id                   text,
    upload_status               varchar(20) not null default 'initializing',
    part_size_bytes             bigint not null,
    part_count                  integer not null,
    upload_parts                jsonb not null default '[]'::jsonb,
    upload_expires_at           timestamp(6) with time zone not null,
    uploaded_time               timestamp(6) with time zone,
    upload_error_message        varchar(1000),
    document_name               varchar(255) not null,
    original_filename           varchar(255) not null,
    file_type                   varchar(255) not null,
    storage_provider            varchar(32) not null default 'minio',
    storage_bucket              varchar(255) not null,
    storage_object_key          text not null,
    content_type                varchar(255) not null,
    file_size                   bigint not null,
    file_sha256                 varchar(64) not null,
    chunk_status                varchar(20) not null default 'unprocessed',
    vector_status               varchar(20) not null default 'pending',
    chunk_count                 integer not null default 0,
    chunk_parameters            jsonb,
    chunk_generation_id         bigint,
    chunk_generation_expires_at timestamp(6) with time zone,
    chunk_error_message         varchar(1000),
    chunk_confirmed_time        timestamp(6) with time zone,
    primary key (id),
    constraint ck_ai_document_chunk_status check (chunk_status in ('unprocessed', 'generating', 'draft', 'confirmed', 'failed')),
    constraint ck_ai_document_vector_status check (vector_status in ('pending', 'processing', 'ready', 'failed', 'stale', 'not_required')),
    constraint ck_ai_document_chunk_count check (chunk_count between 0 and 10000),
    constraint ck_ai_document_chunk_confirmed check (chunk_status <> 'confirmed' or chunk_confirmed_time is not null),
    constraint ck_ai_knowledge_document_status check (data_status in (0, 1)),
    constraint ck_ai_knowledge_document_deleted check (del_flag in (0, 1)),
    constraint ck_ai_knowledge_document_version check (version >= 0),
    constraint ck_ai_knowledge_document_name check (btrim(document_name) <> '' and btrim(original_filename) <> ''),
    constraint ck_ai_knowledge_document_file_type check (file_type <> '' and file_type = lower(file_type)),
    constraint ck_ai_knowledge_document_storage check (storage_provider = 'minio' and btrim(storage_bucket) <> '' and btrim(storage_object_key) <> ''),
    constraint ck_ai_knowledge_document_file check (file_size > 0 and btrim(content_type) <> '' and file_sha256 ~ '^[0-9a-f]{64}$'),
    constraint ck_ai_document_upload check (upload_status in ('initializing', 'uploading', 'completing', 'completed', 'failed', 'cancelled')),
    constraint ck_ai_document_parts check (part_size_bytes between 5242880 and 5368709120
        and part_count between 1 and 10000 and part_count = ((file_size - 1) / part_size_bytes) + 1
        and jsonb_typeof(upload_parts) = 'array'),
    constraint ck_ai_document_upload_session check (upload_status not in ('uploading', 'completing') or upload_id is not null),
    constraint ck_ai_document_upload_complete check (upload_status <> 'completed' or uploaded_time is not null)
);

comment on table ai_knowledge_document is '知识资料：记录基本信息、原始文件的存储位置及上传进度；文件内容保存在对象存储中';
comment on column ai_knowledge_document.is_public is '租户内共享读取标记，默认false；非共享仍按角色范围读取，共享不授予维护权限';
comment on column ai_knowledge_document.id is '主键ID，由应用分配全局唯一的雪花ID，不使用数据库自增';
comment on column ai_knowledge_document.create_time is '创建时间，默认当前时间；保存带时区的时间点';
comment on column ai_knowledge_document.create_by is '文档创建用户ID，可为空；框架本人数据权限通过此字段匹配用户';
comment on column ai_knowledge_document.update_time is '最后修改时间，新增时默认当前时间，后续修改由应用更新';
comment on column ai_knowledge_document.update_by is '最后修改用户ID，可为空';
comment on column ai_knowledge_document.data_status is '正式检索资格：0禁用、1启用，默认禁用但允许管理及召回测试';
comment on column ai_knowledge_document.tenant_id is '所属租户ID，由后端租户上下文确定';
comment on column ai_knowledge_document.org_id is '文档的数据归属部门ID，用于组织数据权限过滤；独立于目录归属部门';
comment on column ai_knowledge_document.del_flag is '逻辑删除标记：0未删除、1已删除，默认0';
comment on column ai_knowledge_document.version is '乐观锁版本号，默认0且不能为负；用于检测并发修改，由应用维护递增';
comment on column ai_knowledge_document.document_name is '文档显示名称，最多255字，不能全为空白；可与上传时的原始文件名不同';
comment on column ai_knowledge_document.original_filename is '上传时的原始文件名，最多255字，不能全为空白；仅用于展示，不作为存储路径';
comment on column ai_knowledge_document.file_type is '原始文件最后一个扩展名，小写且不含点；无扩展名为unknown，仅供列表图标展示，不代表真实格式检测结果';
comment on column ai_knowledge_document.storage_provider is '原始文件使用的对象存储类型，默认minio，当前仅允许minio';
comment on column ai_knowledge_document.storage_bucket is '原始文件所在的对象存储桶名称，不能为空白';
comment on column ai_knowledge_document.storage_object_key is '原始文件在存储桶内的对象键，由后端生成；与存储桶共同定位文件，不保存临时签名URL';
comment on column ai_knowledge_document.content_type is '原始文件的MIME类型，例如application/pdf；不能为空白，类型声明不代表文件格式已通过校验';
comment on column ai_knowledge_document.file_size is '原始文件大小，单位字节，必须大于0';
comment on column ai_knowledge_document.file_sha256 is '原始文件内容的SHA-256摘要，固定64位小写十六进制；用于核验文件内容，不要求唯一';

comment on column ai_knowledge_document.folder_id is '所属分类目录ID。';
comment on column ai_knowledge_document.description is '资料用途说明，最多1000字，可清空。';
comment on column ai_knowledge_document.upload_id is 'MinIO分片上传会话ID，不允许客户端指定。';
comment on column ai_knowledge_document.upload_status is '文件传输状态，用于展示原件上传进度。';
comment on column ai_knowledge_document.part_size_bytes is '固定上传分片大小，单位字节，末片可以较小。';
comment on column ai_knowledge_document.part_count is '上传分片总数，不超过10000。';
comment on column ai_knowledge_document.upload_parts is '后端核实的上传分片清单，JSON持久化。';
comment on column ai_knowledge_document.upload_expires_at is '允许恢复上传的截止时间。';
comment on column ai_knowledge_document.uploaded_time is '完整原件核验通过时间。';
comment on column ai_knowledge_document.upload_error_message is '脱敏上传失败原因，不包含签名地址。';

comment on column ai_knowledge_document.vector_status is '独立向量状态：pending未向量化、processing向量化中、ready已向量化、failed向量化失败、stale已失效、not_required无需向量化；由后端随确认及失效流程维护';
comment on column ai_knowledge_document.chunk_status is '分片状态，与上传及向量处理无关：unprocessed、generating、draft、confirmed、failed';
comment on column ai_knowledge_document.chunk_count is '当前已保存分片数，生成失败不清空旧结果';
comment on column ai_knowledge_document.chunk_parameters is '最近成功生成使用的三级参数，Duration为ISO-8601字符串';
comment on column ai_knowledge_document.chunk_generation_id is '当前生成请求标识，防止超时请求覆盖新结果';
comment on column ai_knowledge_document.chunk_generation_expires_at is '生成请求超时截止时间，过期后允许人工重试';
comment on column ai_knowledge_document.chunk_error_message is '最近一次分片生成的脱敏错误摘要';
comment on column ai_knowledge_document.chunk_confirmed_time is '用户确认当前结果的时间，编辑或合并后清空';

-- 共享资料列表及共享目录定位；常规角色范围复用组织、创建人索引。
create index idx_ai_document_public_created on ai_knowledge_document
    (tenant_id, create_time desc, id desc) include (folder_id) where del_flag = 0 and is_public = true;
create index idx_ai_document_public_folder on ai_knowledge_document
    (tenant_id, folder_id) where del_flag = 0 and is_public = true;

-- 组织数据范围直接按资料归属过滤。
create index idx_ai_knowledge_document_org on ai_knowledge_document
    (tenant_id, org_id, create_time desc, id desc) where del_flag = 0;

create index idx_ai_knowledge_document_created on ai_knowledge_document
    (tenant_id, folder_id, create_time desc, id desc) where del_flag = 0;

-- 过期上传扫描和本人资料列表使用的索引。
create index idx_ai_document_upload_expired on ai_knowledge_document (upload_expires_at, id)
    where del_flag = 0 and upload_status in ('initializing', 'uploading', 'failed');
create index idx_ai_document_creator on ai_knowledge_document (tenant_id, create_by, create_time desc, id desc)
    where del_flag = 0;

-- 目录删除和移动按全局目录ID检查资料引用。
create index idx_ai_document_folder on ai_knowledge_document (folder_id) where del_flag = 0;

-- 分片直接按全局资料ID定位，索引支持预览排序、合并邻接检查和整份结果清理。
create table ai_knowledge_chunk
(
    knowledge_base_id bigint not null,
    processing_version bigint not null default 0,
    id bigint not null primary key,
    create_time timestamp(6) with time zone not null default current_timestamp,
    create_by bigint,
    update_time timestamp(6) with time zone not null default current_timestamp,
    update_by bigint,
    data_status integer not null default 1,
    tenant_id bigint not null,
    org_id bigint not null,
    del_flag smallint not null default 0,
    is_public boolean not null default false,
    document_id bigint not null,
    chunk_index integer not null,
    content text not null,
    token_count integer not null,
    source jsonb not null,
    constraint ck_ai_chunk_content check (btrim(content) <> '' and char_length(content) <= 200000),
    constraint ck_ai_chunk_order check (chunk_index >= 0 and token_count > 0),
    constraint ck_ai_chunk_source check (jsonb_typeof(source) = 'object'),
    constraint ck_ai_chunk_status check (data_status in (0, 1) and del_flag in (0, 1))
);
create unique index uk_ai_chunk_document_order on ai_knowledge_chunk (document_id, chunk_index) where del_flag = 0;
create index idx_ai_chunk_document_cleanup on ai_knowledge_chunk (document_id);
-- 正文全文表达式索引随写入自动维护；查询须使用相同分词函数并包含 del_flag = 0 条件。
create index idx_ai_chunk_content_fts on ai_knowledge_chunk
    using gin (public.etd_knowledge_to_tsvector(content))
    where del_flag = 0;
comment on index idx_ai_chunk_content_fts is '未逻辑删除分片的正文全文GIN索引，采用etd_knowledge_zh中文分词配置';
comment on table ai_knowledge_chunk is '文档分片草稿及确认结果，支持修改和相邻合并，不包含向量';
comment on column ai_knowledge_chunk.is_public is '继承所属资料的共享标记，通过事务同步；不独立设置，不扩大写权限';
comment on column ai_knowledge_chunk.id is '雪花主键';
comment on column ai_knowledge_chunk.document_id is '所属资料ID，无物理外键，由业务事务保证关系';
comment on column ai_knowledge_chunk.chunk_index is '原文顺序，从0开始，合并后允许空号';
comment on column ai_knowledge_chunk.content is '可编辑的权威正文';
comment on column ai_knowledge_chunk.token_count is 'cl100k_base正文Token数，与Docling切分所选分词器可不同';
comment on column ai_knowledge_chunk.source is '来源标题、页码及Docling结构块引用';
comment on column ai_knowledge_chunk.tenant_id is '复制所属资料租户';
comment on column ai_knowledge_chunk.org_id is '复制所属资料组织归属';
comment on column ai_knowledge_chunk.create_time is '创建时间';
comment on column ai_knowledge_chunk.create_by is '继承所属资料创建人，保证本人数据权限一致；实际修改人由update_by记录';
comment on column ai_knowledge_chunk.update_time is '修改时间';
comment on column ai_knowledge_chunk.update_by is '修改人';
comment on column ai_knowledge_chunk.data_status is '启停状态，默认1';
comment on column ai_knowledge_chunk.del_flag is '逻辑删除标记，默认0';




create extension if not exists vector with schema public;

-- 固定768维，模型不同或索引代次不同的向量不得混查。
create table ai_vector_768
(
    id bigint not null primary key,
    content text not null,
    metadata jsonb not null,
    vector vector(768) not null,
    tenant_id bigint generated always as ((metadata->>'tenant_id')::bigint) stored not null,
    knowledge_base_id bigint generated always as ((metadata->>'knowledge_base_id')::bigint) stored not null,
    document_id bigint generated always as ((metadata->>'document_id')::bigint) stored not null,
    chunk_id bigint generated always as ((metadata->>'chunk_id')::bigint) stored not null,
    model_connection_id bigint generated always as ((metadata->>'model_connection_id')::bigint) stored not null,
    index_version bigint generated always as ((metadata->>'index_version')::bigint) stored not null,
    processing_version bigint generated always as ((metadata->>'processing_version')::bigint) stored not null,
    constraint ck_ai_vector_768_content check (btrim(content) <> ''),
    constraint ck_ai_vector_768_metadata check (jsonb_typeof(metadata) = 'object'),
    constraint uk_ai_vector_768_chunk unique (knowledge_base_id, index_version, chunk_id, processing_version)
);
comment on table ai_vector_768 is '768维向量副本，权限与正式发布状态由知识库及文档实时JOIN校验，不缓存公开标记';
comment on column ai_vector_768.index_version is '知识库构建代次，与生效或待生效版本匹配';
comment on column ai_vector_768.processing_version is '向量化时的文档内容轮次，正文变化后失效';
create index idx_ai_vector_768_hnsw on ai_vector_768 using hnsw (vector vector_cosine_ops);
create index idx_ai_vector_768_scope on ai_vector_768 (tenant_id, knowledge_base_id, index_version);
create index idx_ai_vector_768_document on ai_vector_768 (document_id, processing_version);

-- 固定1024维，模型不同或索引代次不同的向量不得混查。
create table ai_vector_1024
(
    id bigint not null primary key,
    content text not null,
    metadata jsonb not null,
    vector vector(1024) not null,
    tenant_id bigint generated always as ((metadata->>'tenant_id')::bigint) stored not null,
    knowledge_base_id bigint generated always as ((metadata->>'knowledge_base_id')::bigint) stored not null,
    document_id bigint generated always as ((metadata->>'document_id')::bigint) stored not null,
    chunk_id bigint generated always as ((metadata->>'chunk_id')::bigint) stored not null,
    model_connection_id bigint generated always as ((metadata->>'model_connection_id')::bigint) stored not null,
    index_version bigint generated always as ((metadata->>'index_version')::bigint) stored not null,
    processing_version bigint generated always as ((metadata->>'processing_version')::bigint) stored not null,
    constraint ck_ai_vector_1024_content check (btrim(content) <> ''),
    constraint ck_ai_vector_1024_metadata check (jsonb_typeof(metadata) = 'object'),
    constraint uk_ai_vector_1024_chunk unique (knowledge_base_id, index_version, chunk_id, processing_version)
);
comment on table ai_vector_1024 is '1024维向量副本，权限与正式发布状态由知识库及文档实时JOIN校验，不缓存公开标记';
comment on column ai_vector_1024.index_version is '知识库构建代次，与生效或待生效版本匹配';
comment on column ai_vector_1024.processing_version is '向量化时的文档内容轮次，正文变化后失效';
create index idx_ai_vector_1024_hnsw on ai_vector_1024 using hnsw (vector vector_cosine_ops);
create index idx_ai_vector_1024_scope on ai_vector_1024 (tenant_id, knowledge_base_id, index_version);
create index idx_ai_vector_1024_document on ai_vector_1024 (document_id, processing_version);

-- 固定1536维，模型不同或索引代次不同的向量不得混查。
create table ai_vector_1536
(
    id bigint not null primary key,
    content text not null,
    metadata jsonb not null,
    vector vector(1536) not null,
    tenant_id bigint generated always as ((metadata->>'tenant_id')::bigint) stored not null,
    knowledge_base_id bigint generated always as ((metadata->>'knowledge_base_id')::bigint) stored not null,
    document_id bigint generated always as ((metadata->>'document_id')::bigint) stored not null,
    chunk_id bigint generated always as ((metadata->>'chunk_id')::bigint) stored not null,
    model_connection_id bigint generated always as ((metadata->>'model_connection_id')::bigint) stored not null,
    index_version bigint generated always as ((metadata->>'index_version')::bigint) stored not null,
    processing_version bigint generated always as ((metadata->>'processing_version')::bigint) stored not null,
    constraint ck_ai_vector_1536_content check (btrim(content) <> ''),
    constraint ck_ai_vector_1536_metadata check (jsonb_typeof(metadata) = 'object'),
    constraint uk_ai_vector_1536_chunk unique (knowledge_base_id, index_version, chunk_id, processing_version)
);
comment on table ai_vector_1536 is '1536维向量副本，权限与正式发布状态由知识库及文档实时JOIN校验，不缓存公开标记';
comment on column ai_vector_1536.index_version is '知识库构建代次，与生效或待生效版本匹配';
comment on column ai_vector_1536.processing_version is '向量化时的文档内容轮次，正文变化后失效';
create index idx_ai_vector_1536_hnsw on ai_vector_1536 using hnsw (vector vector_cosine_ops);
create index idx_ai_vector_1536_scope on ai_vector_1536 (tenant_id, knowledge_base_id, index_version);
create index idx_ai_vector_1536_document on ai_vector_1536 (document_id, processing_version);

-- 覆盖正式/测试召回的知识库与启停筛选，携带权限与内容版本复核字段。
create index idx_ai_document_recall on ai_knowledge_document (knowledge_base_id, data_status, id)
    include (is_public, processing_version)
    where del_flag = 0 and upload_status = 'completed' and chunk_status = 'confirmed';
-- 覆盖库内分页、公开资料入口及全文/向量候选关联。
create index idx_ai_document_base on ai_knowledge_document (knowledge_base_id, create_time desc, id desc) where del_flag = 0;
create index idx_ai_document_public on ai_knowledge_document (knowledge_base_id, folder_id) where is_public = true and del_flag = 0;
create index idx_ai_chunk_base on ai_knowledge_chunk (knowledge_base_id, document_id) where del_flag = 0;
comment on column ai_knowledge_document.knowledge_base_id is '所属知识库，上传与移动须校验分类归属';
comment on column ai_knowledge_document.processing_version is '正文处理轮次，正文编辑和重新生成递增，元数据编辑不变';
comment on column ai_knowledge_chunk.knowledge_base_id is '所属知识库，由资料继承';
comment on column ai_knowledge_chunk.processing_version is '生成时的资料轮次，召回最终以资料当前轮次校验';
comment on column ai_knowledge_base.id is '全局雪花主键';
comment on column ai_knowledge_base.tenant_id is '所属租户，普通用户由框架隔离';
comment on column ai_knowledge_base.org_id is '知识库组织归属，子资源权限由本库继承';
comment on column ai_knowledge_base.create_time is '创建时间点';
comment on column ai_knowledge_base.create_by is '创建人，同时参与知识库本人数据范围';
comment on column ai_knowledge_base.update_time is '修改时间点';
comment on column ai_knowledge_base.update_by is '最后修改人';
comment on column ai_knowledge_base.data_status is '知识库启停状态：0禁用、1启用';
comment on column ai_knowledge_base.del_flag is '逻辑删除：0未删除、1已删除';
comment on column ai_knowledge_base.index_lease_until is '后台任务租约，工作线程重启或失效后可重新领取';
comment on column ai_vector_768.id is '独立向量记录雪花ID，一个分片可跨索引代次拥有多条记录';
comment on column ai_vector_768.content is '模型输入正文副本，权威正文以分片表为准';
comment on column ai_vector_768.metadata is '后端写入的租户、知识库、文档、分片、模型及版本信息';
comment on column ai_vector_768.vector is '固定768维非零向量';
comment on column ai_vector_768.tenant_id is '由metadata生成的租户ID';
comment on column ai_vector_768.knowledge_base_id is '由metadata生成的知识库ID，用于检索边界过滤';
comment on column ai_vector_768.document_id is '所属文档ID，检索时实时校验文档启停及公开状态';
comment on column ai_vector_768.chunk_id is '分片ID，业务层保证关联完整性';
comment on column ai_vector_768.model_connection_id is '生成向量所用的租户模型连接ID';
comment on column ai_vector_1024.id is '独立向量记录雪花ID，一个分片可跨索引代次拥有多条记录';
comment on column ai_vector_1024.content is '模型输入正文副本，权威正文以分片表为准';
comment on column ai_vector_1024.metadata is '后端写入的租户、知识库、文档、分片、模型及版本信息';
comment on column ai_vector_1024.vector is '固定1024维非零向量';
comment on column ai_vector_1024.tenant_id is '由metadata生成的租户ID';
comment on column ai_vector_1024.knowledge_base_id is '由metadata生成的知识库ID，用于检索边界过滤';
comment on column ai_vector_1024.document_id is '所属文档ID，检索时实时校验文档启停及公开状态';
comment on column ai_vector_1024.chunk_id is '分片ID，业务层保证关联完整性';
comment on column ai_vector_1024.model_connection_id is '生成向量所用的租户模型连接ID';
comment on column ai_vector_1536.id is '独立向量记录雪花ID，一个分片可跨索引代次拥有多条记录';
comment on column ai_vector_1536.content is '模型输入正文副本，权威正文以分片表为准';
comment on column ai_vector_1536.metadata is '后端写入的租户、知识库、文档、分片、模型及版本信息';
comment on column ai_vector_1536.vector is '固定1536维非零向量';
comment on column ai_vector_1536.tenant_id is '由metadata生成的租户ID';
comment on column ai_vector_1536.knowledge_base_id is '由metadata生成的知识库ID，用于检索边界过滤';
comment on column ai_vector_1536.document_id is '所属文档ID，检索时实时校验文档启停及公开状态';
comment on column ai_vector_1536.chunk_id is '分片ID，业务层保证关联完整性';
comment on column ai_vector_1536.model_connection_id is '生成向量所用的租户模型连接ID';
commit;
