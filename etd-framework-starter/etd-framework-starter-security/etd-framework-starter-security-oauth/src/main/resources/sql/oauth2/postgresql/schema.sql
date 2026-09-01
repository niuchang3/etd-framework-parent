/*==============================================================*/
/* table: oauth2_client                                         */
/*==============================================================*/
create table if not exists oauth2_client
(
    id                              bigint not null,
    resource_code                   varchar(100) not null,
    client_id                       varchar(100) not null,
    client_name                     varchar(200) not null,
    client_secret                   varchar(300),
    client_type                     varchar(30) not null,
    client_level                    varchar(30) not null,
    client_authentication_methods   text not null,
    authorization_grant_types       text not null,
    redirect_uris                   text,
    post_logout_redirect_uris       text,
    trusted                         boolean default false,
    require_pkce                    boolean default true,
    require_authorization_consent   boolean default true,
    access_token_ttl                int,
    refresh_token_ttl               int,
    enabled                         boolean default true,
    locked                          boolean default false,
    built_in                        boolean default false,
    create_time                     timestamp(6) with time zone,
    data_status                     int,
    primary key (id)
);

comment on table oauth2_client is 'OAuth2客户端表';
comment on column oauth2_client.id is '主键id';
comment on column oauth2_client.resource_code is '资源编码，业务系统可映射为应用编码';
comment on column oauth2_client.client_id is '客户端id';
comment on column oauth2_client.client_name is '客户端名称';
comment on column oauth2_client.client_secret is '客户端密钥';
comment on column oauth2_client.client_type is '客户端类型：public/confidential';
comment on column oauth2_client.client_level is '客户端级别：official/third_party/internal_service';
comment on column oauth2_client.client_authentication_methods is '客户端认证方式';
comment on column oauth2_client.authorization_grant_types is '授权模式';
comment on column oauth2_client.redirect_uris is '授权回调地址';
comment on column oauth2_client.post_logout_redirect_uris is '退出登录回调地址';
comment on column oauth2_client.trusted is '是否可信客户端';
comment on column oauth2_client.require_pkce is '是否要求PKCE';
comment on column oauth2_client.require_authorization_consent is '是否需要授权确认';
comment on column oauth2_client.access_token_ttl is '访问令牌有效期，单位秒';
comment on column oauth2_client.refresh_token_ttl is '刷新令牌有效期，单位秒';
comment on column oauth2_client.enabled is '是否启用';
comment on column oauth2_client.locked is '是否锁定';
comment on column oauth2_client.built_in is '是否系统内置客户端';
comment on column oauth2_client.create_time is '创建时间';
comment on column oauth2_client.data_status is '数据状态';

create unique index if not exists uk_oauth2_client_client_id
    on oauth2_client (client_id);

create index if not exists idx_oauth2_client_resource_code
    on oauth2_client (resource_code);

/*==============================================================*/
/* table: oauth2_scope                                          */
/*==============================================================*/
create table if not exists oauth2_scope
(
    id            bigint not null,
    resource_code varchar(100) not null,
    scope_code    varchar(100) not null,
    scope_name    varchar(100) not null,
    scope_group   varchar(100),
    scope_type    varchar(30),
    description   varchar(300),
    built_in      boolean default false,
    enabled       boolean default true,
    sort          int,
    create_time   timestamp(6) with time zone,
    data_status   int,
    primary key (id)
);

comment on table oauth2_scope is 'OAuth2权限范围表';
comment on column oauth2_scope.id is '主键id';
comment on column oauth2_scope.resource_code is '资源编码，业务系统可映射为应用编码';
comment on column oauth2_scope.scope_code is '权限范围编码';
comment on column oauth2_scope.scope_name is '权限范围名称';
comment on column oauth2_scope.scope_group is '权限范围分组';
comment on column oauth2_scope.scope_type is '权限范围类型：read/write/manage';
comment on column oauth2_scope.description is '权限范围描述';
comment on column oauth2_scope.built_in is '是否系统内置权限范围';
comment on column oauth2_scope.enabled is '是否启用';
comment on column oauth2_scope.sort is '排序';
comment on column oauth2_scope.create_time is '创建时间';
comment on column oauth2_scope.data_status is '数据状态';

create unique index if not exists uk_oauth2_scope_resource_code
    on oauth2_scope (resource_code, scope_code);

/*==============================================================*/
/* table: oauth2_client_scope                                   */
/*==============================================================*/
create table if not exists oauth2_client_scope
(
    id          bigint not null,
    client_id   bigint not null,
    scope_id    bigint not null,
    create_time timestamp(6) with time zone,
    data_status int,
    primary key (id)
);

comment on table oauth2_client_scope is 'OAuth2客户端权限范围关系表';
comment on column oauth2_client_scope.id is '主键id';
comment on column oauth2_client_scope.client_id is '客户端主键id';
comment on column oauth2_client_scope.scope_id is '权限范围id';
comment on column oauth2_client_scope.create_time is '创建时间';
comment on column oauth2_client_scope.data_status is '数据状态';

create unique index if not exists uk_oauth2_client_scope
    on oauth2_client_scope (client_id, scope_id);

create index if not exists idx_oauth2_client_scope_scope_id
    on oauth2_client_scope (scope_id);

/*==============================================================*/
/* table: oauth2_authorization                                  */
/*==============================================================*/
create table if not exists oauth2_authorization
(
    id                              varchar(100) not null,
    registered_client_id            varchar(100) not null,
    principal_name                  varchar(200) not null,
    authorization_grant_type        varchar(100) not null,
    tenant_id                       bigint,
    resource_code                   varchar(100),
    authorized_scopes               text,
    attributes                      text,
    state                           varchar(500),
    authorization_code_value        text,
    authorization_code_issued_at    timestamp(6) with time zone,
    authorization_code_expires_at   timestamp(6) with time zone,
    authorization_code_metadata     text,
    access_token_value              text,
    access_token_issued_at          timestamp(6) with time zone,
    access_token_expires_at         timestamp(6) with time zone,
    access_token_metadata           text,
    access_token_type               varchar(100),
    access_token_scopes             text,
    oidc_id_token_value             text,
    oidc_id_token_issued_at         timestamp(6) with time zone,
    oidc_id_token_expires_at        timestamp(6) with time zone,
    oidc_id_token_metadata          text,
    refresh_token_value             text,
    refresh_token_issued_at         timestamp(6) with time zone,
    refresh_token_expires_at        timestamp(6) with time zone,
    refresh_token_metadata          text,
    user_code_value                 text,
    user_code_issued_at             timestamp(6) with time zone,
    user_code_expires_at            timestamp(6) with time zone,
    user_code_metadata              text,
    device_code_value               text,
    device_code_issued_at           timestamp(6) with time zone,
    device_code_expires_at          timestamp(6) with time zone,
    device_code_metadata            text,
    create_time                     timestamp(6) with time zone,
    data_status                     int,
    primary key (id)
);

comment on table oauth2_authorization is 'OAuth2授权记录表';
comment on column oauth2_authorization.id is '主键id';
comment on column oauth2_authorization.registered_client_id is '注册客户端id';
comment on column oauth2_authorization.principal_name is '授权主体名称';
comment on column oauth2_authorization.authorization_grant_type is '授权模式';
comment on column oauth2_authorization.tenant_id is '租户id';
comment on column oauth2_authorization.resource_code is '资源编码';
comment on column oauth2_authorization.authorized_scopes is '已授权权限范围';
comment on column oauth2_authorization.attributes is '授权属性';
comment on column oauth2_authorization.state is '授权请求状态';
comment on column oauth2_authorization.authorization_code_value is '授权码值';
comment on column oauth2_authorization.authorization_code_issued_at is '授权码签发时间';
comment on column oauth2_authorization.authorization_code_expires_at is '授权码过期时间';
comment on column oauth2_authorization.authorization_code_metadata is '授权码元数据';
comment on column oauth2_authorization.access_token_value is '访问令牌值';
comment on column oauth2_authorization.access_token_issued_at is '访问令牌签发时间';
comment on column oauth2_authorization.access_token_expires_at is '访问令牌过期时间';
comment on column oauth2_authorization.access_token_metadata is '访问令牌元数据';
comment on column oauth2_authorization.access_token_type is '访问令牌类型';
comment on column oauth2_authorization.access_token_scopes is '访问令牌权限范围';
comment on column oauth2_authorization.oidc_id_token_value is 'OIDC ID Token值';
comment on column oauth2_authorization.oidc_id_token_issued_at is 'OIDC ID Token签发时间';
comment on column oauth2_authorization.oidc_id_token_expires_at is 'OIDC ID Token过期时间';
comment on column oauth2_authorization.oidc_id_token_metadata is 'OIDC ID Token元数据';
comment on column oauth2_authorization.refresh_token_value is '刷新令牌值';
comment on column oauth2_authorization.refresh_token_issued_at is '刷新令牌签发时间';
comment on column oauth2_authorization.refresh_token_expires_at is '刷新令牌过期时间';
comment on column oauth2_authorization.refresh_token_metadata is '刷新令牌元数据';
comment on column oauth2_authorization.user_code_value is '设备授权用户码值';
comment on column oauth2_authorization.user_code_issued_at is '设备授权用户码签发时间';
comment on column oauth2_authorization.user_code_expires_at is '设备授权用户码过期时间';
comment on column oauth2_authorization.user_code_metadata is '设备授权用户码元数据';
comment on column oauth2_authorization.device_code_value is '设备码值';
comment on column oauth2_authorization.device_code_issued_at is '设备码签发时间';
comment on column oauth2_authorization.device_code_expires_at is '设备码过期时间';
comment on column oauth2_authorization.device_code_metadata is '设备码元数据';
comment on column oauth2_authorization.create_time is '创建时间';
comment on column oauth2_authorization.data_status is '数据状态';

create index if not exists idx_oauth2_authorization_registered_client_id
    on oauth2_authorization (registered_client_id);

create index if not exists idx_oauth2_authorization_principal_name
    on oauth2_authorization (principal_name);

create index if not exists idx_oauth2_authorization_tenant_resource
    on oauth2_authorization (tenant_id, resource_code);

create index if not exists idx_oauth2_authorization_access_token_expires_at
    on oauth2_authorization (access_token_expires_at);

create index if not exists idx_oauth2_authorization_refresh_token_expires_at
    on oauth2_authorization (refresh_token_expires_at);

/*==============================================================*/
/* table: oauth2_authorization_consent                          */
/*==============================================================*/
create table if not exists oauth2_authorization_consent
(
    registered_client_id varchar(100) not null,
    principal_name       varchar(200) not null,
    tenant_id            bigint not null,
    resource_code        varchar(100) not null,
    authorities          text not null,
    create_time          timestamp(6) with time zone,
    data_status          int,
    primary key (registered_client_id, principal_name, tenant_id, resource_code)
);

comment on table oauth2_authorization_consent is 'OAuth2用户授权确认表';
comment on column oauth2_authorization_consent.registered_client_id is '注册客户端id';
comment on column oauth2_authorization_consent.principal_name is '授权主体名称';
comment on column oauth2_authorization_consent.tenant_id is '租户id';
comment on column oauth2_authorization_consent.resource_code is '资源编码';
comment on column oauth2_authorization_consent.authorities is '已同意授权范围';
comment on column oauth2_authorization_consent.create_time is '创建时间';
comment on column oauth2_authorization_consent.data_status is '数据状态';
