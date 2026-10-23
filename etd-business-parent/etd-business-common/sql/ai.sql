/*==============================================================*/
/* SaaS 模型、知识库目录及资料上传的初始结构。           */
/* 本脚本描述当前最终结构，由部署流程显式执行，不在应用启动时执行。 */
/*==============================================================*/

-- 当前仅维护模型、目录和原件上传，不安装分片检索所需扩展。
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

comment on table ai_knowledge_folder is '知识库目录：按部门维护的分类目录树，资料直接归属于目录';
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
    primary key (id),
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
comment on column ai_knowledge_document.id is '主键ID，由应用分配全局唯一的雪花ID，不使用数据库自增';
comment on column ai_knowledge_document.create_time is '创建时间，默认当前时间；保存带时区的时间点';
comment on column ai_knowledge_document.create_by is '文档创建用户ID，可为空；框架本人数据权限通过此字段匹配用户';
comment on column ai_knowledge_document.update_time is '最后修改时间，新增时默认当前时间，后续修改由应用更新';
comment on column ai_knowledge_document.update_by is '最后修改用户ID，可为空';
comment on column ai_knowledge_document.data_status is '启停状态：0禁用、1启用，默认1';
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

commit;
