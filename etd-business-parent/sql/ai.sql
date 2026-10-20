/*==============================================================*/
/* AI 数据库检索扩展与中英文分词配置。                          */
/* 在 AI 数据库执行，本节先安装扩展，再执行后续建表。          */
/* 数据库镜像须已安装 vector、zhparser、SCWS 及其词库。         */
/* 不包含系统字典初始化，不由应用启动流程自动执行。            */
/*==============================================================*/
begin;

-- 界面可能已启用扩展；此处只复用已安装扩展，不下载安装文件。
create extension if not exists vector;
create extension if not exists zhparser;

-- SCWS 统一切分中英文，再以英文词干词典规范化；中文词项保持原词，英文词形归一。
create text search configuration public.ai_zh_v1 (parser = zhparser);
alter text search configuration public.ai_zh_v1
    add mapping for n, v, a, i, e, l, j, x, m, t with english_stem;

comment on text search configuration public.ai_zh_v1 is
    'AI 中英文统一分词配置 v1；SCWS 切词及英文词干归一，变更后必须重算全文索引列';

-- 固定 regconfig 使生成列不受连接默认语言影响；配置视为不可变，变更需重算已存词项。
create function public.ai_search_vector(title text, content text)
returns tsvector
language sql
immutable
as $function$
    select pg_catalog.setweight(
               pg_catalog.to_tsvector('public.ai_zh_v1'::regconfig, coalesce(title, '')), 'A')
           || pg_catalog.setweight(
               pg_catalog.to_tsvector('public.ai_zh_v1'::regconfig, coalesce(content, '')), 'B');
$function$;

comment on function public.ai_search_vector(text, text) is
    '生成统一中英文全文词项：标题权重 A、正文权重 B，供分片 STORED 列及 GIN 索引使用';

-- 查询和索引使用相同分词规则；普通输入不作为 tsquery 语法执行。
create function public.ai_search_query(query_text text)
returns tsquery
language sql
immutable
as $function$
    select pg_catalog.plainto_tsquery('public.ai_zh_v1'::regconfig, coalesce(query_text, ''));
$function$;

comment on function public.ai_search_query(text) is
    '按索引相同规则生成中英文全文查询；调用方须对空词项返回空候选并附加租户授权条件';

commit;

/*==============================================================*/
/* SaaS 模型、知识库、分片及向量检索的完整初始结构。           */
/* 本脚本描述当前最终结构，由部署流程显式执行，不在应用启动时执行。 */
/*==============================================================*/

-- 扩展和检索函数由本文件前半部分创建；本节定义首次建库表结构。
begin;
set local search_path = public, pg_catalog;

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
comment on column ai_model.embedding is '平台登记的模型支持维度集合，向量模型必填且为不重复正整数；非向量模型为空；知识库实际维度须同时受该集合及系统存储维度约束';
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
create table ai_knowledge_folder
(
    id                          bigint not null,
    create_time                 timestamp(6) with time zone not null default current_timestamp,
    create_by                   bigint,
    update_time                 timestamp(6) with time zone not null default current_timestamp,
    update_by                   bigint,
    data_status                 integer not null default 1,
    tenant_id                   bigint not null,
    org_id                      bigint not null,
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

comment on table ai_knowledge_folder is '知识库目录：按部门维护的分类目录树，目录下创建知识库，文档上传到知识库中';
comment on column ai_knowledge_folder.id is '主键ID，由应用分配全局唯一的雪花ID，不使用数据库自增';
comment on column ai_knowledge_folder.create_time is '创建时间，默认当前时间；保存带时区的时间点';
comment on column ai_knowledge_folder.create_by is '目录创建用户ID，可为空；本人数据范围是否可见由框架数据权限规则决定';
comment on column ai_knowledge_folder.update_time is '最后修改时间，新增时默认当前时间，后续修改由应用更新';
comment on column ai_knowledge_folder.update_by is '最后修改用户ID，可为空';
comment on column ai_knowledge_folder.data_status is '启停状态：0禁用、1启用，默认1';
comment on column ai_knowledge_folder.tenant_id is '所属租户ID，由后端租户上下文确定';
comment on column ai_knowledge_folder.org_id is '目录所属部门ID，采用前端传入值；访问范围由框架数据权限控制，不在AI服务中校验部门归属';
comment on column ai_knowledge_folder.del_flag is '逻辑删除标记：0未删除、1已删除，默认0';
comment on column ai_knowledge_folder.version is '乐观锁版本号，默认0且不能为负；用于检测并发修改，由应用维护递增';
comment on column ai_knowledge_folder.parent_id is '父目录ID，关联本表id；根目录为空，父子目录须属于同一租户和部门，不允许目录循环引用';
comment on column ai_knowledge_folder.folder_name is '目录名称，最多100字；不能有首尾空格，同租户同部门同父目录的未删除目录不能重名';
comment on column ai_knowledge_folder.description is '目录用途或分类说明，可为空，最多500字';
comment on column ai_knowledge_folder.sort_order is '同级目录排序值，默认0且不能为负；数值越小越靠前，相同时按ID排序';

-- PostgreSQL 16 使用 NULLS NOT DISTINCT，根目录 parent_id 为空时也禁止同级重名。
create unique index uk_ai_knowledge_folder_name on ai_knowledge_folder
    (tenant_id, org_id, parent_id, folder_name) nulls not distinct where del_flag = 0;
-- 支持同级目录排序及递归树查询，关联完整性由目录业务层校验。
create index idx_ai_knowledge_folder_parent on ai_knowledge_folder
    (tenant_id, org_id, parent_id, sort_order, id);

-- 所选部门列表过滤及全树排序，状态为可选筛选字段。
create index idx_ai_knowledge_folder_org_sort on ai_knowledge_folder
    (tenant_id, org_id, sort_order, id) include (data_status) where del_flag = 0;

-- 不指定部门时按租户读取全树，并保持目录排序。
create index idx_ai_knowledge_folder_tenant_sort on ai_knowledge_folder
    (tenant_id, sort_order, id) include (data_status) where del_flag = 0;

-- 本人范围按创建人过滤，部门作为可选附加筛选字段。
create index idx_ai_knowledge_folder_creator_sort on ai_knowledge_folder
    (tenant_id, create_by, sort_order, id) include (org_id) where del_flag = 0;

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
    del_flag                    smallint not null default 0,
    version                     bigint not null default 0,
    knowledge_name              varchar(100) not null,
    description                 varchar(1000),
    retrieval_mode              varchar(16) not null default 'keyword',
    embedding_model_connection_id bigint,
    embedding_model_id          bigint,
    embedding                   integer,
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
    constraint ck_ai_knowledge_base_name check (knowledge_name = btrim(knowledge_name) and knowledge_name <> ''),
    constraint ck_ai_knowledge_base_mode check (retrieval_mode in ('keyword', 'vector', 'hybrid')),
    constraint ck_ai_knowledge_base_embedding check (
        (embedding_model_connection_id is null and embedding_model_id is null and embedding is null)
        or (embedding_model_connection_id is not null and embedding_model_id is not null and embedding is not null
            and embedding in (768, 1024, 1536))),
    constraint ck_ai_knowledge_base_chunk_size check (chunk_max_tokens > 0 and chunk_overlap_tokens >= 0 and chunk_overlap_tokens < chunk_max_tokens),
    constraint ck_ai_knowledge_base_top_k check (keyword_top_k > 0 and vector_top_k > 0 and retrieval_top_k > 0
        and case retrieval_mode
            when 'keyword' then retrieval_top_k <= keyword_top_k
            when 'vector' then retrieval_top_k <= vector_top_k
            else retrieval_top_k::bigint <= keyword_top_k::bigint + vector_top_k::bigint end),
    constraint ck_ai_knowledge_base_threshold check (similarity_threshold is null or similarity_threshold between 0 and 1)
);

comment on table ai_knowledge_base is '知识库：归属于目录的文档集合，统一配置文档分片参数和全文、向量或混合检索策略';
comment on column ai_knowledge_base.id is '主键ID，由应用分配全局唯一的雪花ID，不使用数据库自增';
comment on column ai_knowledge_base.create_time is '创建时间，默认当前时间；保存带时区的时间点';
comment on column ai_knowledge_base.create_by is '创建用户ID，可为空';
comment on column ai_knowledge_base.update_time is '最后修改时间，新增时默认当前时间，后续修改由应用更新';
comment on column ai_knowledge_base.update_by is '最后修改用户ID，可为空';
comment on column ai_knowledge_base.data_status is '启停状态：0禁用、1启用，默认1';
comment on column ai_knowledge_base.tenant_id is '所属租户ID，由后端租户上下文确定';
comment on column ai_knowledge_base.del_flag is '逻辑删除标记：0未删除、1已删除，默认0';
comment on column ai_knowledge_base.version is '乐观锁版本号，默认0且不能为负；用于检测并发修改，由应用维护递增';
comment on column ai_knowledge_base.knowledge_name is '知识库显示名称，最多100字；不能有首尾空格，同租户同目录的未删除知识库不能重名';
comment on column ai_knowledge_base.description is '知识库的用途和内容范围说明，可为空，最多1000字';
comment on column ai_knowledge_base.retrieval_mode is '检索模式：keyword全文、vector向量、hybrid混合，默认keyword；保存模式不代表检索功能已经可用';
comment on column ai_knowledge_base.embedding_model_connection_id is '向量模型连接器ID，关联ai_model_connection.id；与模型ID和维度同时填写或留空';
comment on column ai_knowledge_base.embedding_model_id is '连接器绑定的平台模型ID，由后端确定；由业务层校验所选连接器确实绑定该模型';
comment on column ai_knowledge_base.embedding is '知识库独立选择的向量输出维度，仅允许768、1024、1536；实际调用必须显式传递且服务商须支持，与embedding_model_connection_id同时填写或留空';
comment on column ai_knowledge_base.chunk_max_tokens is '文档分片的目标Token上限，必须大于0；还需由业务校验是否符合模型输入限制';
comment on column ai_knowledge_base.chunk_overlap_tokens is '相邻分片重复保留的Token数量，不能为负，且必须小于chunk_max_tokens';
comment on column ai_knowledge_base.keyword_top_k is '全文检索分支最多召回的候选分片数，必须大于0；不是最终返回数量';
comment on column ai_knowledge_base.vector_top_k is '向量检索分支最多召回的候选分片数，必须大于0；不是最终返回数量';
comment on column ai_knowledge_base.retrieval_top_k is '最终返回的分片数量上限，必须大于0；不能超过当前模式的候选数，混合模式按两个分支的候选数之和计算';
comment on column ai_knowledge_base.similarity_threshold is '向量检索的最低相似度，范围0～1；为空时不额外限制得分，不用于全文得分或混合排序得分';

create unique index uk_ai_knowledge_base_name on ai_knowledge_base (tenant_id, folder_id, knowledge_name) where del_flag = 0;
create index idx_ai_knowledge_base_created on ai_knowledge_base (tenant_id, create_time desc, id desc) where del_flag = 0;
create index idx_ai_knowledge_base_connection on ai_knowledge_base (embedding_model_connection_id) where del_flag = 0;

comment on column ai_knowledge_base.org_id is '知识库维护归属部门ID，用于管理权限过滤；文档的数据归属部门单独记录';
comment on column ai_knowledge_base.folder_id is '所属目录ID，关联ai_knowledge_folder.id；必须属于同一租户，移动知识库只改变目录归属';
-- 支持目录下的知识库列表及引用检查，关联完整性由业务层校验。
create index idx_ai_knowledge_base_folder on ai_knowledge_base (folder_id, create_time desc, id desc);
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
    org_id                      bigint not null,
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

comment on table ai_knowledge_document is '知识库文档：记录原始文件的存储位置、公开范围及当前解析和向量化进度；文件内容保存在对象存储中';
comment on column ai_knowledge_document.id is '主键ID，由应用分配全局唯一的雪花ID，不使用数据库自增';
comment on column ai_knowledge_document.create_time is '创建时间，默认当前时间；保存带时区的时间点';
comment on column ai_knowledge_document.create_by is '文档创建用户ID，可为空；框架本人数据权限通过此字段匹配用户';
comment on column ai_knowledge_document.update_time is '最后修改时间，新增时默认当前时间，后续修改由应用更新';
comment on column ai_knowledge_document.update_by is '最后修改用户ID，可为空';
comment on column ai_knowledge_document.data_status is '启停状态：0禁用、1启用，默认1';
comment on column ai_knowledge_document.tenant_id is '所属租户ID，由后端租户上下文确定';
comment on column ai_knowledge_document.org_id is '文档的数据归属部门ID，用于组织数据权限过滤；独立于知识库维护部门，分片及向量保存相同值';
comment on column ai_knowledge_document.del_flag is '逻辑删除标记：0未删除、1已删除，默认0';
comment on column ai_knowledge_document.version is '乐观锁版本号，默认0且不能为负；用于检测并发修改，由应用维护递增';
comment on column ai_knowledge_document.knowledge_base_id is '所属知识库ID，关联ai_knowledge_base.id，必须属于同一租户';
comment on column ai_knowledge_document.is_public is '是否允许租户内公开检索：true公开、false按数据权限访问，默认false；应用修改时须在同一事务同步分片和向量的公开标记';
comment on column ai_knowledge_document.document_name is '文档显示名称，最多255字，不能全为空白；可与上传时的原始文件名不同';
comment on column ai_knowledge_document.original_filename is '上传时的原始文件名，最多255字，不能全为空白；仅用于展示，不作为存储路径';
comment on column ai_knowledge_document.storage_provider is '原始文件使用的对象存储类型，默认minio，当前仅允许minio';
comment on column ai_knowledge_document.storage_bucket is '原始文件所在的对象存储桶名称，不能为空白';
comment on column ai_knowledge_document.storage_object_key is '原始文件在存储桶内的对象键，由后端生成；与存储桶共同定位文件，不保存临时签名URL';
comment on column ai_knowledge_document.content_type is '原始文件的MIME类型，例如application/pdf；不能为空白，类型声明不代表文件格式已通过校验';
comment on column ai_knowledge_document.file_size is '原始文件大小，单位字节，必须大于0';
comment on column ai_knowledge_document.file_sha256 is '原始文件内容的SHA-256摘要，固定64位小写十六进制；用于核验文件内容，不要求唯一';
comment on column ai_knowledge_document.processing_status is '文档处理状态：uploaded已上传未解析、pending待处理、parsing解析中、preview待确认分片、embedding向量化中、ready可检索、failed失败；默认uploaded';
comment on column ai_knowledge_document.processing_version is '文档处理轮次，默认0且不能为负；每次重新处理由应用递增，异步任务须核对轮次以免旧结果覆盖新结果';
comment on column ai_knowledge_document.processing_owner is '当前处理任务的唯一租约ID；解析中或向量化中必须填写，离开这两个状态时与租约到期时间一起清空';
comment on column ai_knowledge_document.processing_lease_until is '当前处理任务的租约到期时间；解析中或向量化中必填且晚于处理开始时间，到期后可由其他任务接管';
comment on column ai_knowledge_document.processing_started_time is '本轮处理开始时间；解析中或向量化中必填，判断任务是否超时应使用processing_lease_until';
comment on column ai_knowledge_document.parser_version is '本轮解析器及解析规则的版本，由后端记录，用于追溯原始文件如何被解析';
comment on column ai_knowledge_document.splitter_version is '本轮分词器及分片规则的版本，由后端记录，用于追溯正文如何被拆分';
comment on column ai_knowledge_document.chunk_max_tokens is '本轮实际采用的分片Token上限快照；生成分片后必填，不随知识库配置修改而自动变化';
comment on column ai_knowledge_document.chunk_overlap_tokens is '本轮实际采用的分片重叠Token数快照；与chunk_max_tokens同时填写或留空，非负且小于分片上限';
comment on column ai_knowledge_document.error_message is '本轮处理失败的错误摘要，最多1000字；保存前须脱敏，不记录密钥或完整模型请求';
comment on column ai_knowledge_document.processed_time is '本轮处理成功或失败的结束时间，尚未结束时可为空';

-- 组织数据范围过滤不关联知识库，公开及发布状态仍需独立判断。
create index idx_ai_knowledge_document_org on ai_knowledge_document
    (tenant_id, org_id, knowledge_base_id, create_time desc, id desc) where del_flag = 0;

create index idx_ai_knowledge_document_created on ai_knowledge_document
    (tenant_id, knowledge_base_id, create_time desc, id desc) where del_flag = 0;
create index idx_ai_knowledge_document_public on ai_knowledge_document
    (tenant_id, knowledge_base_id, create_time desc, id desc) where del_flag = 0 and data_status = 1 and is_public;
-- 文档列表索引已以租户、知识库开头，无需重复建立同前缀索引。
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
    org_id                      bigint not null,
    is_public                   boolean not null default false,
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
    constraint ck_ai_knowledge_chunk_content check (btrim(content) <> '' and token_count > 0),
    constraint ck_ai_knowledge_chunk_position check (processing_version >= 0 and chunk_index >= 0),
    constraint ck_ai_knowledge_chunk_location check (jsonb_typeof(source_location) = 'object'),
    constraint ck_ai_knowledge_chunk_embedding check (embedding_status in ('not_required', 'pending', 'ready', 'failed') and (embedding_status <> 'ready' or embedded_time is not null))
);

comment on table ai_knowledge_chunk is '文档分片：保存文档拆分后的正文、来源位置和向量化状态，支持全文检索；向量另存于对应维度的向量表';
comment on column ai_knowledge_chunk.id is '主键ID，由应用分配全局唯一的雪花ID，不使用数据库自增';
comment on column ai_knowledge_chunk.create_time is '创建时间，默认当前时间；保存带时区的时间点';
comment on column ai_knowledge_chunk.create_by is '创建用户ID，可为空';
comment on column ai_knowledge_chunk.update_time is '最后修改时间，新增时默认当前时间，后续修改由应用更新';
comment on column ai_knowledge_chunk.update_by is '最后修改用户ID，可为空';
comment on column ai_knowledge_chunk.data_status is '启停状态：0禁用、1启用，默认1';
comment on column ai_knowledge_chunk.tenant_id is '所属租户ID，由后端租户上下文确定';
comment on column ai_knowledge_chunk.org_id is '分片所属部门ID，复制自所属文档；由业务层校验并与文档同步，用于直接按组织过滤分片';
comment on column ai_knowledge_chunk.is_public is '文档公开标记的冗余副本，默认false；应用须与文档及向量在同一事务同步，由业务层校验一致性';
comment on column ai_knowledge_chunk.del_flag is '逻辑删除标记：0未删除、1已删除，默认0';
comment on column ai_knowledge_chunk.version is '乐观锁版本号，默认0且不能为负；用于检测并发修改，由应用维护递增';
comment on column ai_knowledge_chunk.knowledge_base_id is '所属知识库ID，必须与所属文档的knowledge_base_id一致';
comment on column ai_knowledge_chunk.document_id is '所属文档ID，关联ai_knowledge_document.id；租户、知识库、部门及公开标记须与文档一致';
comment on column ai_knowledge_chunk.processing_version is '生成本分片时的文档处理轮次，不能为负；检索时仍需核对它是否属于文档当前有效轮次';
comment on column ai_knowledge_chunk.chunk_index is '本轮文档内的分片顺序，从0开始；同一文档同一处理轮次内不能重复';
comment on column ai_knowledge_chunk.title is '分片的章节标题，默认空字符串；无章节标题时由应用使用文档显示名称，全文检索时标题权重高于正文';
comment on column ai_knowledge_chunk.content is '分片正文，不能全为空白；用于预览、编辑、全文检索和生成向量，是分片内容的权威来源';
comment on column ai_knowledge_chunk.search_vector is '全文检索词项，由数据库根据title和content自动生成；标题或正文变化时自动重算，应用不直接写入';
comment on column ai_knowledge_chunk.token_count is '分片正文经实际分词器计算的Token数量，必须大于0';
comment on column ai_knowledge_chunk.source_location is '分片在原文中的位置，JSON对象，默认空对象；可记录页码、章节路径等定位信息';
comment on column ai_knowledge_chunk.vector_id is '分片对应的全局唯一检索ID，由应用预分配雪花ID；用作Spring AI Document.id和向量表主键，与本表id分开记录';
comment on column ai_knowledge_chunk.embedding_status is '分片向量化状态：not_required无需向量、pending待处理、ready已完成、failed失败，默认pending；纯全文场景由应用填写not_required';
comment on column ai_knowledge_chunk.error_message is '当前分片向量化失败的错误摘要，最多1000字；保存前须脱敏';
comment on column ai_knowledge_chunk.embedded_time is '当前分片正文成功写入向量的时间；embedding_status为ready时必填';

-- 可与全文GIN索引组合过滤组织范围，无需为取得org_id关联文档。
create index idx_ai_knowledge_chunk_org on ai_knowledge_chunk
    (tenant_id, org_id, knowledge_base_id, document_id, processing_version)
    where del_flag = 0 and data_status = 1;

-- 全文查询必须同时限制启停、删除及文档当前发布轮次；索引本身不承担租户授权。
-- 租户内公开召回直接过滤分片，不为判断公开状态关联文档。
create index idx_ai_knowledge_chunk_public on ai_knowledge_chunk
    (tenant_id, knowledge_base_id, document_id, processing_version)
    where del_flag = 0 and data_status = 1 and is_public;

create index idx_ai_knowledge_chunk_fulltext on ai_knowledge_chunk using gin (search_vector)
    where del_flag = 0 and data_status = 1;
-- 分片顺序唯一键支持按租户、知识库、文档及轮次读取，无需重复建立同前缀索引。

/*==============================================================*/
/* ai_vector_768：固定768维，基础写入字段为id、content、metadata、vector。 */
/*==============================================================*/
create table ai_vector_768
(
    id bigint not null primary key,
    content text not null,
    metadata jsonb not null,
    vector vector(768) not null,
    tenant_id bigint generated always as ((metadata->>'tenant_id')::bigint) stored not null,
    org_id bigint generated always as ((metadata->>'org_id')::bigint) stored not null,
    is_public boolean generated always as ((metadata->>'is_public')::boolean) stored not null,
    knowledge_base_id bigint generated always as ((metadata->>'knowledge_base_id')::bigint) stored not null,
    document_id bigint generated always as ((metadata->>'document_id')::bigint) stored not null,
    chunk_id bigint generated always as ((metadata->>'chunk_id')::bigint) stored not null,
    model_connection_id bigint generated always as ((metadata->>'model_connection_id')::bigint) stored not null,
    processing_version bigint generated always as ((metadata->>'processing_version')::bigint) stored not null,
    embedding integer generated always as (768) stored not null,
    constraint ck_ai_vector_768_content check (btrim(content) <> ''),
    constraint ck_ai_vector_768_metadata check (
        jsonb_typeof(metadata) = 'object'
        and metadata ? 'tenant_id' and jsonb_typeof(metadata->'tenant_id') = 'string'
        and (metadata->>'tenant_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'org_id' and jsonb_typeof(metadata->'org_id') = 'string'
        and (metadata->>'org_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'is_public' and jsonb_typeof(metadata->'is_public') = 'boolean'
        and metadata ? 'knowledge_base_id' and jsonb_typeof(metadata->'knowledge_base_id') = 'string'
        and (metadata->>'knowledge_base_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'document_id' and jsonb_typeof(metadata->'document_id') = 'string'
        and (metadata->>'document_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'chunk_id' and jsonb_typeof(metadata->'chunk_id') = 'string'
        and (metadata->>'chunk_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'model_connection_id' and jsonb_typeof(metadata->'model_connection_id') = 'string'
        and (metadata->>'model_connection_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'processing_version' and jsonb_typeof(metadata->'processing_version') = 'string'
        and (metadata->>'processing_version') ~ '^(0|[1-9][0-9]*)$'
    )
);

comment on table ai_vector_768 is '768维向量数据：保存文档分片的向量及检索所需副本；正文以ai_knowledge_chunk为准，归属字段由metadata自动生成，关联完整性由业务层校验';
comment on column ai_vector_768.id is '向量记录主键，等于ai_knowledge_chunk.vector_id；由应用写入雪花ID，不是分片表的id';
comment on column ai_vector_768.content is '生成该向量时使用的分片正文副本，不能全为空白；权威正文保存在ai_knowledge_chunk.content';
comment on column ai_vector_768.metadata is '检索元数据JSON对象，必须包含tenant_id、org_id、is_public、knowledge_base_id、document_id、chunk_id、model_connection_id、processing_version；ID及轮次使用十进制字符串，is_public使用布尔值';
comment on column ai_vector_768.vector is '模型生成的768维向量，维度必须恰好为768，所用模型须与知识库绑定配置一致';
comment on column ai_vector_768.tenant_id is '所属租户ID，由数据库从metadata.tenant_id自动生成，应用不直接写入此列';
comment on column ai_vector_768.org_id is '文档数据归属部门ID，由数据库从metadata.org_id自动生成；须与所属分片一致，可直接用于组织过滤';
comment on column ai_vector_768.is_public is '文档公开标记副本，由数据库从metadata.is_public自动生成；应用须同步修改文档、分片及metadata，由业务层校验一致性';
comment on column ai_vector_768.knowledge_base_id is '所属知识库ID，由数据库从metadata.knowledge_base_id自动生成，须与所属分片一致';
comment on column ai_vector_768.document_id is '所属文档ID，由数据库从metadata.document_id自动生成，须与所属分片一致';
comment on column ai_vector_768.chunk_id is '所属分片ID，由数据库从metadata.chunk_id自动生成，关联ai_knowledge_chunk.id';
comment on column ai_vector_768.model_connection_id is '生成向量的模型连接器ID，由数据库从metadata.model_connection_id自动生成，须与知识库绑定的模型一致';
comment on column ai_vector_768.processing_version is '所属文档处理轮次，由数据库从metadata.processing_version自动生成，须与所属分片一致；检索时仍需核对文档当前有效轮次';
comment on column ai_vector_768.embedding is '向量维度，由数据库固定生成为768；供业务层校验知识库选择的维度，应用不直接写入此列';

create index idx_ai_vector_768_hnsw on ai_vector_768 using hnsw (vector vector_cosine_ops);
-- 支持按租户、知识库、文档及处理轮次清理向量。
create index idx_ai_vector_768_scope on ai_vector_768
    (tenant_id, knowledge_base_id, document_id, processing_version);
-- 组织过滤直接读取生成列，租户、组织及知识库组成查询前缀。
create index idx_ai_vector_768_org on ai_vector_768
    (tenant_id, org_id, knowledge_base_id);
-- 租户内公开向量召回的过滤索引，公开标记必须与文档一致。
create index idx_ai_vector_768_public on ai_vector_768
    (tenant_id, knowledge_base_id) where is_public;
-- 支持metadata JSON路径过滤；真实召回SQL仍需校验业务发布状态。
create index idx_ai_vector_768_metadata on ai_vector_768 using gin (metadata jsonb_path_ops);

/*==============================================================*/
/* ai_vector_1024：固定1024维，基础写入字段为id、content、metadata、vector。 */
/*==============================================================*/
create table ai_vector_1024
(
    id bigint not null primary key,
    content text not null,
    metadata jsonb not null,
    vector vector(1024) not null,
    tenant_id bigint generated always as ((metadata->>'tenant_id')::bigint) stored not null,
    org_id bigint generated always as ((metadata->>'org_id')::bigint) stored not null,
    is_public boolean generated always as ((metadata->>'is_public')::boolean) stored not null,
    knowledge_base_id bigint generated always as ((metadata->>'knowledge_base_id')::bigint) stored not null,
    document_id bigint generated always as ((metadata->>'document_id')::bigint) stored not null,
    chunk_id bigint generated always as ((metadata->>'chunk_id')::bigint) stored not null,
    model_connection_id bigint generated always as ((metadata->>'model_connection_id')::bigint) stored not null,
    processing_version bigint generated always as ((metadata->>'processing_version')::bigint) stored not null,
    embedding integer generated always as (1024) stored not null,
    constraint ck_ai_vector_1024_content check (btrim(content) <> ''),
    constraint ck_ai_vector_1024_metadata check (
        jsonb_typeof(metadata) = 'object'
        and metadata ? 'tenant_id' and jsonb_typeof(metadata->'tenant_id') = 'string'
        and (metadata->>'tenant_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'org_id' and jsonb_typeof(metadata->'org_id') = 'string'
        and (metadata->>'org_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'is_public' and jsonb_typeof(metadata->'is_public') = 'boolean'
        and metadata ? 'knowledge_base_id' and jsonb_typeof(metadata->'knowledge_base_id') = 'string'
        and (metadata->>'knowledge_base_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'document_id' and jsonb_typeof(metadata->'document_id') = 'string'
        and (metadata->>'document_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'chunk_id' and jsonb_typeof(metadata->'chunk_id') = 'string'
        and (metadata->>'chunk_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'model_connection_id' and jsonb_typeof(metadata->'model_connection_id') = 'string'
        and (metadata->>'model_connection_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'processing_version' and jsonb_typeof(metadata->'processing_version') = 'string'
        and (metadata->>'processing_version') ~ '^(0|[1-9][0-9]*)$'
    )
);

comment on table ai_vector_1024 is '1024维向量数据：保存文档分片的向量及检索所需副本；正文以ai_knowledge_chunk为准，归属字段由metadata自动生成，关联完整性由业务层校验';
comment on column ai_vector_1024.id is '向量记录主键，等于ai_knowledge_chunk.vector_id；由应用写入雪花ID，不是分片表的id';
comment on column ai_vector_1024.content is '生成该向量时使用的分片正文副本，不能全为空白；权威正文保存在ai_knowledge_chunk.content';
comment on column ai_vector_1024.metadata is '检索元数据JSON对象，必须包含tenant_id、org_id、is_public、knowledge_base_id、document_id、chunk_id、model_connection_id、processing_version；ID及轮次使用十进制字符串，is_public使用布尔值';
comment on column ai_vector_1024.vector is '模型生成的1024维向量，维度必须恰好为1024，所用模型须与知识库绑定配置一致';
comment on column ai_vector_1024.tenant_id is '所属租户ID，由数据库从metadata.tenant_id自动生成，应用不直接写入此列';
comment on column ai_vector_1024.org_id is '文档数据归属部门ID，由数据库从metadata.org_id自动生成；须与所属分片一致，可直接用于组织过滤';
comment on column ai_vector_1024.is_public is '文档公开标记副本，由数据库从metadata.is_public自动生成；应用须同步修改文档、分片及metadata，由业务层校验一致性';
comment on column ai_vector_1024.knowledge_base_id is '所属知识库ID，由数据库从metadata.knowledge_base_id自动生成，须与所属分片一致';
comment on column ai_vector_1024.document_id is '所属文档ID，由数据库从metadata.document_id自动生成，须与所属分片一致';
comment on column ai_vector_1024.chunk_id is '所属分片ID，由数据库从metadata.chunk_id自动生成，关联ai_knowledge_chunk.id';
comment on column ai_vector_1024.model_connection_id is '生成向量的模型连接器ID，由数据库从metadata.model_connection_id自动生成，须与知识库绑定的模型一致';
comment on column ai_vector_1024.processing_version is '所属文档处理轮次，由数据库从metadata.processing_version自动生成，须与所属分片一致；检索时仍需核对文档当前有效轮次';
comment on column ai_vector_1024.embedding is '向量维度，由数据库固定生成为1024；供业务层校验知识库选择的维度，应用不直接写入此列';

create index idx_ai_vector_1024_hnsw on ai_vector_1024 using hnsw (vector vector_cosine_ops);
-- 支持按租户、知识库、文档及处理轮次清理向量。
create index idx_ai_vector_1024_scope on ai_vector_1024
    (tenant_id, knowledge_base_id, document_id, processing_version);
-- 组织过滤直接读取生成列，租户、组织及知识库组成查询前缀。
create index idx_ai_vector_1024_org on ai_vector_1024
    (tenant_id, org_id, knowledge_base_id);
-- 租户内公开向量召回的过滤索引，公开标记必须与文档一致。
create index idx_ai_vector_1024_public on ai_vector_1024
    (tenant_id, knowledge_base_id) where is_public;
-- 支持metadata JSON路径过滤；真实召回SQL仍需校验业务发布状态。
create index idx_ai_vector_1024_metadata on ai_vector_1024 using gin (metadata jsonb_path_ops);

/*==============================================================*/
/* ai_vector_1536：固定1536维，基础写入字段为id、content、metadata、vector。 */
/*==============================================================*/
create table ai_vector_1536
(
    id bigint not null primary key,
    content text not null,
    metadata jsonb not null,
    vector vector(1536) not null,
    tenant_id bigint generated always as ((metadata->>'tenant_id')::bigint) stored not null,
    org_id bigint generated always as ((metadata->>'org_id')::bigint) stored not null,
    is_public boolean generated always as ((metadata->>'is_public')::boolean) stored not null,
    knowledge_base_id bigint generated always as ((metadata->>'knowledge_base_id')::bigint) stored not null,
    document_id bigint generated always as ((metadata->>'document_id')::bigint) stored not null,
    chunk_id bigint generated always as ((metadata->>'chunk_id')::bigint) stored not null,
    model_connection_id bigint generated always as ((metadata->>'model_connection_id')::bigint) stored not null,
    processing_version bigint generated always as ((metadata->>'processing_version')::bigint) stored not null,
    embedding integer generated always as (1536) stored not null,
    constraint ck_ai_vector_1536_content check (btrim(content) <> ''),
    constraint ck_ai_vector_1536_metadata check (
        jsonb_typeof(metadata) = 'object'
        and metadata ? 'tenant_id' and jsonb_typeof(metadata->'tenant_id') = 'string'
        and (metadata->>'tenant_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'org_id' and jsonb_typeof(metadata->'org_id') = 'string'
        and (metadata->>'org_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'is_public' and jsonb_typeof(metadata->'is_public') = 'boolean'
        and metadata ? 'knowledge_base_id' and jsonb_typeof(metadata->'knowledge_base_id') = 'string'
        and (metadata->>'knowledge_base_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'document_id' and jsonb_typeof(metadata->'document_id') = 'string'
        and (metadata->>'document_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'chunk_id' and jsonb_typeof(metadata->'chunk_id') = 'string'
        and (metadata->>'chunk_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'model_connection_id' and jsonb_typeof(metadata->'model_connection_id') = 'string'
        and (metadata->>'model_connection_id') ~ '^[1-9][0-9]*$'
        and metadata ? 'processing_version' and jsonb_typeof(metadata->'processing_version') = 'string'
        and (metadata->>'processing_version') ~ '^(0|[1-9][0-9]*)$'
    )
);

comment on table ai_vector_1536 is '1536维向量数据：保存文档分片的向量及检索所需副本；正文以ai_knowledge_chunk为准，归属字段由metadata自动生成，关联完整性由业务层校验';
comment on column ai_vector_1536.id is '向量记录主键，等于ai_knowledge_chunk.vector_id；由应用写入雪花ID，不是分片表的id';
comment on column ai_vector_1536.content is '生成该向量时使用的分片正文副本，不能全为空白；权威正文保存在ai_knowledge_chunk.content';
comment on column ai_vector_1536.metadata is '检索元数据JSON对象，必须包含tenant_id、org_id、is_public、knowledge_base_id、document_id、chunk_id、model_connection_id、processing_version；ID及轮次使用十进制字符串，is_public使用布尔值';
comment on column ai_vector_1536.vector is '模型生成的1536维向量，维度必须恰好为1536，所用模型须与知识库绑定配置一致';
comment on column ai_vector_1536.tenant_id is '所属租户ID，由数据库从metadata.tenant_id自动生成，应用不直接写入此列';
comment on column ai_vector_1536.org_id is '文档数据归属部门ID，由数据库从metadata.org_id自动生成；须与所属分片一致，可直接用于组织过滤';
comment on column ai_vector_1536.is_public is '文档公开标记副本，由数据库从metadata.is_public自动生成；应用须同步修改文档、分片及metadata，由业务层校验一致性';
comment on column ai_vector_1536.knowledge_base_id is '所属知识库ID，由数据库从metadata.knowledge_base_id自动生成，须与所属分片一致';
comment on column ai_vector_1536.document_id is '所属文档ID，由数据库从metadata.document_id自动生成，须与所属分片一致';
comment on column ai_vector_1536.chunk_id is '所属分片ID，由数据库从metadata.chunk_id自动生成，关联ai_knowledge_chunk.id';
comment on column ai_vector_1536.model_connection_id is '生成向量的模型连接器ID，由数据库从metadata.model_connection_id自动生成，须与知识库绑定的模型一致';
comment on column ai_vector_1536.processing_version is '所属文档处理轮次，由数据库从metadata.processing_version自动生成，须与所属分片一致；检索时仍需核对文档当前有效轮次';
comment on column ai_vector_1536.embedding is '向量维度，由数据库固定生成为1536；供业务层校验知识库选择的维度，应用不直接写入此列';

create index idx_ai_vector_1536_hnsw on ai_vector_1536 using hnsw (vector vector_cosine_ops);
-- 支持按租户、知识库、文档及处理轮次清理向量。
create index idx_ai_vector_1536_scope on ai_vector_1536
    (tenant_id, knowledge_base_id, document_id, processing_version);
-- 组织过滤直接读取生成列，租户、组织及知识库组成查询前缀。
create index idx_ai_vector_1536_org on ai_vector_1536
    (tenant_id, org_id, knowledge_base_id);
-- 租户内公开向量召回的过滤索引，公开标记必须与文档一致。
create index idx_ai_vector_1536_public on ai_vector_1536
    (tenant_id, knowledge_base_id) where is_public;
-- 支持metadata JSON路径过滤；真实召回SQL仍需校验业务发布状态。
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

comment on table ai_file_upload is '文件上传任务：记录大文件分片传输、合并结果和过期时间，与文档解析及向量化任务分开管理';
comment on column ai_file_upload.id is '主键ID，由应用分配全局唯一的雪花ID，不使用数据库自增';
comment on column ai_file_upload.create_time is '创建时间，默认当前时间；保存带时区的时间点';
comment on column ai_file_upload.create_by is '上传任务创建用户ID，可为空；用于记录任务发起人';
comment on column ai_file_upload.update_time is '最后修改时间，新增时默认当前时间，后续修改由应用更新';
comment on column ai_file_upload.update_by is '最后修改用户ID，可为空';
comment on column ai_file_upload.data_status is '启停状态：0禁用、1启用，默认1';
comment on column ai_file_upload.tenant_id is '所属租户ID，由后端租户上下文确定';
comment on column ai_file_upload.del_flag is '逻辑删除标记：0未删除、1已删除，默认0';
comment on column ai_file_upload.version is '乐观锁版本号，默认0且不能为负；用于检测并发修改，由应用维护递增';
comment on column ai_file_upload.knowledge_base_id is '上传目标知识库ID，关联ai_knowledge_base.id，必须属于同一租户';
comment on column ai_file_upload.replace_document_id is '本次上传要替换的文档ID，可为空；填写时须属于同一租户和目标知识库，并同时填写replace_version';
comment on column ai_file_upload.replace_version is '开始上传时待替换文档的乐观锁版本，不能为负；用于在完成上传时检测文档是否已被其他操作修改';
comment on column ai_file_upload.original_filename is '待上传文件的原始文件名，最多255字；仅用于展示，不作为存储路径';
comment on column ai_file_upload.file_size is '本次上传声明的文件大小，单位字节，必须大于0；合并完成后仍需核验实际大小';
comment on column ai_file_upload.bucket is '上传文件的目标对象存储桶，由后端确定';
comment on column ai_file_upload.object_key is '上传文件的目标对象键，由后端生成；同租户同存储桶内不能重复';
comment on column ai_file_upload.upload_id is '对象存储服务返回的分片上传任务ID；上传中必须有值，用于提交分片和合并文件';
comment on column ai_file_upload.state is '文件传输状态：initializing初始化中、uploading上传中、completed已完成、cancelled已取消或过期；不表示文档解析进度';
comment on column ai_file_upload.document_id is '上传完成后生成或更新的文档ID；仅completed状态允许且必须有值，用于重复确认时返回同一结果';
comment on column ai_file_upload.part_size_bytes is '每个上传分片的固定大小，单位字节，范围5MiB～5GiB；最后一片可小于此值，与文档正文分片无关';
comment on column ai_file_upload.part_count is '上传分片总数，范围1～10000；必须等于文件大小除以分片大小后向上取整的结果';
comment on column ai_file_upload.url_expiry_seconds is '单个上传分片签名URL的有效时长，单位秒，范围1～604800；与整个上传任务的过期时间分开记录';
comment on column ai_file_upload.concurrency is '本次上传采用的客户端并发分片数，必须大于等于1；记录任务参数，不直接控制服务端线程数';
comment on column ai_file_upload.max_retries is '客户端上传每个分片时允许的额外重试次数，不能为负；0表示失败后不额外重试';
comment on column ai_file_upload.expires_at is '整个上传任务的过期时间；到期后应停止签发上传URL和合并文件，并安排未完成上传的清理';




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

comment on table ai_storage_cleanup is '文件清理任务：记录待删除的原始文件或待中止的分片上传，清理失败后可继续重试';
comment on column ai_storage_cleanup.id is '主键ID，由应用分配全局唯一的雪花ID，不使用数据库自增';
comment on column ai_storage_cleanup.create_time is '创建时间，默认当前时间；保存带时区的时间点';
comment on column ai_storage_cleanup.create_by is '发起文件清理的用户ID，系统任务发起时可为空';
comment on column ai_storage_cleanup.update_time is '最后修改时间，新增时默认当前时间，后续修改由应用更新';
comment on column ai_storage_cleanup.update_by is '最后修改清理任务的用户ID，系统任务修改时可为空';
comment on column ai_storage_cleanup.data_status is '启停状态：0禁用、1启用，默认1';
comment on column ai_storage_cleanup.tenant_id is '所属租户ID，由后端租户上下文确定';
comment on column ai_storage_cleanup.del_flag is '逻辑删除标记：0未删除、1已删除，默认0；清理成功后由应用物理删除任务记录';
comment on column ai_storage_cleanup.bucket is '待清理文件或分片上传所在的对象存储桶';
comment on column ai_storage_cleanup.object_key is '待清理文件的对象键，与bucket共同定位对象；不保存临时签名URL';
comment on column ai_storage_cleanup.upload_id is '待中止的对象存储分片上传任务ID，可为空；未保存ID时可按对象键查找未完成上传';
comment on column ai_storage_cleanup.abort_multipart is '是否需要中止该对象尚未完成的分片上传：true需要、false不需要';
comment on column ai_storage_cleanup.next_attempt_at is '允许执行下一次清理的时间，默认当前时间；失败后由应用推迟此时间再重试';
comment on column ai_storage_cleanup.attempts is '已失败的清理次数，默认0且不能为负；失败时由应用递增，任务保留以便重试';

commit;
