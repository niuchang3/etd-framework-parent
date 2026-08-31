/*==============================================================*/
/* table: sys_tenant                                            */
/*==============================================================*/
create table if not exists sys_tenant
(
    id                bigint not null,
    create_time       timestamp(6),
    create_by         bigint,
    update_time       timestamp(6),
    update_by         bigint,
    data_status       int,
    del_flag          smallint not null default 0,
    logo              varchar(200),
    tenant_name       varchar(100),
    description       varchar(200),
    credit_code       varchar(100),
    tenant_type       varchar(100),
    tenant_admin_user bigint,
    locked            boolean,
    enabled           boolean,
    primary key (id)
);

comment on table sys_tenant is '系统租户表';
comment on column sys_tenant.id is '主键id';
comment on column sys_tenant.create_time is '创建时间';
comment on column sys_tenant.create_by is '创建人';
comment on column sys_tenant.update_time is '修改时间';
comment on column sys_tenant.update_by is '修改人';
comment on column sys_tenant.data_status is '数据状态';
comment on column sys_tenant.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_tenant.logo is 'logo地址';
comment on column sys_tenant.tenant_name is '租户名称';
comment on column sys_tenant.description is '租户描述';
comment on column sys_tenant.credit_code is '信用代码';
comment on column sys_tenant.tenant_type is '企业类型';
comment on column sys_tenant.tenant_admin_user is '租户管理员';
comment on column sys_tenant.locked is '租户是否锁定';
comment on column sys_tenant.enabled is '租户启用';

/*==============================================================*/
/* table: sys_user                                              */
/*==============================================================*/
create table if not exists sys_user
(
    id          bigint not null,
    create_time timestamp(6),
    create_by   bigint,
    update_time timestamp(6),
    update_by   bigint,
    data_status int,
    del_flag    smallint not null default 0,
    tenant_id   bigint not null,
    account     varchar(32),
    mobile      varchar(20),
    password    varchar(100),
    user_name   varchar(20),
    birthday    date,
    gender      int,
    avatar      varchar(200),
    nick_name   varchar(100),
    locked      boolean,
    enabled     boolean,
    primary key (id)
);

comment on table sys_user is '系统用户表';
comment on column sys_user.id is '主键id';
comment on column sys_user.create_time is '创建时间';
comment on column sys_user.create_by is '创建人';
comment on column sys_user.update_time is '修改时间';
comment on column sys_user.update_by is '修改人';
comment on column sys_user.data_status is '数据状态';
comment on column sys_user.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_user.tenant_id is '租户id';
comment on column sys_user.account is '账号';
comment on column sys_user.mobile is '手机号码';
comment on column sys_user.password is '密码';
comment on column sys_user.user_name is '用户姓名';
comment on column sys_user.birthday is '生日';
comment on column sys_user.gender is '性别';
comment on column sys_user.avatar is '用户头像';
comment on column sys_user.nick_name is '用户昵称';
comment on column sys_user.locked is '账号锁定';
comment on column sys_user.enabled is '账号启用';

/*==============================================================*/
/* table: sys_role                                              */
/*==============================================================*/
create table if not exists sys_role
(
    id              bigint not null,
    create_time     timestamp(6),
    create_by       bigint,
    update_time     timestamp(6),
    update_by       bigint,
    data_status     int,
    del_flag        smallint not null default 0,
    tenant_id       bigint not null,
    built_in        boolean,
    role_name       varchar(20),
    role_code       varchar(50),
    role_desc       varchar(200),
    permission_type varchar(30),
    primary key (id)
);

comment on table sys_role is '系统角色表';
comment on column sys_role.id is '主键id';
comment on column sys_role.create_time is '创建时间';
comment on column sys_role.create_by is '创建人';
comment on column sys_role.update_time is '修改时间';
comment on column sys_role.update_by is '修改人';
comment on column sys_role.data_status is '数据状态';
comment on column sys_role.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_role.tenant_id is '租户id';
comment on column sys_role.built_in is '是否内置';
comment on column sys_role.role_name is '角色名称';
comment on column sys_role.role_code is '角色code';
comment on column sys_role.role_desc is '角色描述';
comment on column sys_role.permission_type is '数据权限范围类型：1不限制，2仅本人，3仅组织，4组织及下级组织，5自定义跨组织';

/*==============================================================*/
/* table: sys_user_role_rel                                     */
/*==============================================================*/
create table if not exists sys_user_role_rel
(
    id          bigint not null,
    create_time timestamp(6),
    create_by   bigint,
    update_time timestamp(6),
    update_by   bigint,
    data_status int,
    del_flag    smallint not null default 0,
    tenant_id   bigint not null,
    user_id     bigint,
    role_id     bigint,
    primary key (id)
);

comment on table sys_user_role_rel is '系统用户与角色的关系表';
comment on column sys_user_role_rel.id is '主键id';
comment on column sys_user_role_rel.create_time is '创建时间';
comment on column sys_user_role_rel.create_by is '创建人';
comment on column sys_user_role_rel.update_time is '修改时间';
comment on column sys_user_role_rel.update_by is '修改人';
comment on column sys_user_role_rel.data_status is '数据状态';
comment on column sys_user_role_rel.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_user_role_rel.tenant_id is '租户id';
comment on column sys_user_role_rel.user_id is '用户id';
comment on column sys_user_role_rel.role_id is '角色id';

/*==============================================================*/
/* table: sys_user_org_rel                                      */
/*==============================================================*/
create table if not exists sys_user_org_rel
(
    id          bigint not null,
    create_time timestamp(6),
    create_by   bigint,
    update_time timestamp(6),
    update_by   bigint,
    data_status int,
    del_flag    smallint not null default 0,
    tenant_id   bigint not null,
    user_id     bigint not null,
    org_id      bigint not null,
    primary_org boolean default false,
    primary key (id)
);

comment on table sys_user_org_rel is '系统用户与组织的关系表';
comment on column sys_user_org_rel.id is '主键id';
comment on column sys_user_org_rel.create_time is '创建时间';
comment on column sys_user_org_rel.create_by is '创建人';
comment on column sys_user_org_rel.update_time is '修改时间';
comment on column sys_user_org_rel.update_by is '修改人';
comment on column sys_user_org_rel.data_status is '数据状态';
comment on column sys_user_org_rel.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_user_org_rel.tenant_id is '租户id';
comment on column sys_user_org_rel.user_id is '用户id';
comment on column sys_user_org_rel.org_id is '组织id';
comment on column sys_user_org_rel.primary_org is '是否主组织';

create index if not exists idx_sys_user_org_rel_tenant_user
    on sys_user_org_rel (tenant_id, user_id);

/*==============================================================*/
/* table: sys_role_menu_rel                                     */
/*==============================================================*/
create table if not exists sys_role_menu_rel
(
    id          bigint not null,
    create_time timestamp(6),
    create_by   bigint,
    update_time timestamp(6),
    update_by   bigint,
    data_status int,
    del_flag    smallint not null default 0,
    tenant_id   bigint not null,
    role_id     bigint,
    menu_id     bigint,
    access_level     smallint not null default 1,
    primary key (id)
);

comment on table sys_role_menu_rel is '系统角色与菜单的关系表';
comment on column sys_role_menu_rel.id is '主键id';
comment on column sys_role_menu_rel.create_time is '创建时间';
comment on column sys_role_menu_rel.create_by is '创建人';
comment on column sys_role_menu_rel.update_time is '修改时间';
comment on column sys_role_menu_rel.update_by is '修改人';
comment on column sys_role_menu_rel.data_status is '数据状态';
comment on column sys_role_menu_rel.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_role_menu_rel.tenant_id is '租户id';
comment on column sys_role_menu_rel.role_id is '角色id';
comment on column sys_role_menu_rel.menu_id is '菜单id';
comment on column sys_role_menu_rel.access_level is '角色菜单访问级别：1只读，2读写';

/*==============================================================*/
/* table: sys_role_org_rel                                      */
/*==============================================================*/
create table if not exists sys_role_org_rel
(
    id          bigint not null,
    create_time timestamp(6),
    create_by   bigint,
    update_time timestamp(6),
    update_by   bigint,
    data_status int,
    del_flag    smallint not null default 0,
    tenant_id   bigint not null,
    role_id     bigint not null,
    org_id      bigint not null,
    primary key (id)
);

comment on table sys_role_org_rel is '系统角色与自定义数据权限组织的关系表';
comment on column sys_role_org_rel.id is '主键id';
comment on column sys_role_org_rel.create_time is '创建时间';
comment on column sys_role_org_rel.create_by is '创建人';
comment on column sys_role_org_rel.update_time is '修改时间';
comment on column sys_role_org_rel.update_by is '修改人';
comment on column sys_role_org_rel.data_status is '数据状态';
comment on column sys_role_org_rel.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_role_org_rel.tenant_id is '租户id';
comment on column sys_role_org_rel.role_id is '角色id';
comment on column sys_role_org_rel.org_id is '组织id';

create index if not exists idx_sys_role_org_rel_tenant_role
    on sys_role_org_rel (tenant_id, role_id);

/*==============================================================*/
/* table: sys_tenant_menu_rel                                   */
/*==============================================================*/
create table if not exists sys_tenant_menu_rel
(
    id          bigint not null,
    create_time timestamp(6),
    create_by   bigint,
    update_time timestamp(6),
    update_by   bigint,
    data_status int,
    del_flag    smallint not null default 0,
    tenant_id   bigint not null,
    menu_id     bigint not null,
    primary key (id)
);

comment on table sys_tenant_menu_rel is '系统租户与菜单的关系表';
comment on column sys_tenant_menu_rel.id is '主键id';
comment on column sys_tenant_menu_rel.create_time is '创建时间';
comment on column sys_tenant_menu_rel.create_by is '创建人';
comment on column sys_tenant_menu_rel.update_time is '修改时间';
comment on column sys_tenant_menu_rel.update_by is '修改人';
comment on column sys_tenant_menu_rel.data_status is '数据状态';
comment on column sys_tenant_menu_rel.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_tenant_menu_rel.tenant_id is '租户id';
comment on column sys_tenant_menu_rel.menu_id is '菜单id';

create index if not exists idx_sys_tenant_menu_rel_tenant_menu
    on sys_tenant_menu_rel (tenant_id, menu_id);

/*==============================================================*/
/* table: sys_api                                               */
/*==============================================================*/
create table if not exists sys_api
(
    id             bigint not null,
    create_time    timestamp(6),
    create_by      bigint,
    update_time    timestamp(6),
    update_by      bigint,
    data_status    int,
    del_flag       smallint not null default 0,
    api_name       varchar(100) not null,
    request_method varchar(10) not null,
    request_path   varchar(200) not null,
    access_level   smallint not null default 1,
    enabled        boolean default true,
    primary key (id)
);

comment on table sys_api is '系统接口权限资源表';
comment on column sys_api.id is '主键id';
comment on column sys_api.create_time is '创建时间';
comment on column sys_api.create_by is '创建人';
comment on column sys_api.update_time is '修改时间';
comment on column sys_api.update_by is '修改人';
comment on column sys_api.data_status is '数据状态';
comment on column sys_api.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_api.api_name is '接口名称';
comment on column sys_api.request_method is 'HTTP请求方法';
comment on column sys_api.request_path is 'Spring请求路径模式';
comment on column sys_api.access_level is '接口要求访问级别：1读取，2写入';
comment on column sys_api.enabled is '是否启用';

create index if not exists idx_sys_api_request
    on sys_api (request_method, request_path);

/*==============================================================*/
/* table: sys_menu_api_rel                                      */
/*==============================================================*/
create table if not exists sys_menu_api_rel
(
    id          bigint not null,
    create_time timestamp(6),
    create_by   bigint,
    update_time timestamp(6),
    update_by   bigint,
    data_status int,
    del_flag    smallint not null default 0,
    menu_id     bigint not null,
    api_id      bigint not null,
    primary key (id)
);

comment on table sys_menu_api_rel is '系统菜单与接口的关系表';
comment on column sys_menu_api_rel.id is '主键id';
comment on column sys_menu_api_rel.create_time is '创建时间';
comment on column sys_menu_api_rel.create_by is '创建人';
comment on column sys_menu_api_rel.update_time is '修改时间';
comment on column sys_menu_api_rel.update_by is '修改人';
comment on column sys_menu_api_rel.data_status is '数据状态';
comment on column sys_menu_api_rel.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_menu_api_rel.menu_id is '菜单id';
comment on column sys_menu_api_rel.api_id is '接口id';

create index if not exists idx_sys_menu_api_rel_menu
    on sys_menu_api_rel (menu_id);

create index if not exists idx_sys_menu_api_rel_api
    on sys_menu_api_rel (api_id);

/*==============================================================*/
/* table: sys_menus                                             */
/*==============================================================*/
create table if not exists sys_menus
(
    id          bigint not null,
    parent_id   bigint,
    create_time timestamp(6),
    create_by   bigint,
    update_time timestamp(6),
    update_by   bigint,
    data_status int,
    del_flag    smallint not null default 0,
    menu_name   varchar(10),
    menu_path   varchar(100),
    menu_router varchar(100),
    menu_icon   varchar(200),
    menu_type   varchar(20),
    sort        int,
    primary key (id)
);

comment on table sys_menus is '系统菜单';
comment on column sys_menus.id is '主键id';
comment on column sys_menus.parent_id is '父级菜单';
comment on column sys_menus.create_time is '创建时间';
comment on column sys_menus.create_by is '创建人';
comment on column sys_menus.update_time is '修改时间';
comment on column sys_menus.update_by is '修改人';
comment on column sys_menus.data_status is '数据状态';
comment on column sys_menus.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_menus.menu_name is '菜单名称';
comment on column sys_menus.menu_path is '菜单path';
comment on column sys_menus.menu_router is '菜单路由';
comment on column sys_menus.menu_icon is '菜单图标';
comment on column sys_menus.menu_type is '菜单类型';
comment on column sys_menus.sort is '排序';

/*==============================================================*/
/* table: sys_dict_type                                         */
/*==============================================================*/
create table if not exists sys_dict_type
(
    id          bigint not null,
    create_time timestamp(6),
    create_by   bigint,
    update_time timestamp(6),
    update_by   bigint,
    data_status int,
    del_flag    smallint not null default 0,
    type_code   varchar(100) not null,
    type_name   varchar(100) not null,
    built_in    boolean not null default false,
    enabled     boolean not null default true,
    remark      varchar(500),
    primary key (id)
);

comment on table sys_dict_type is '系统字典类型表';
comment on column sys_dict_type.id is '主键id';
comment on column sys_dict_type.create_time is '创建时间';
comment on column sys_dict_type.create_by is '创建人';
comment on column sys_dict_type.update_time is '修改时间';
comment on column sys_dict_type.update_by is '修改人';
comment on column sys_dict_type.data_status is '数据状态';
comment on column sys_dict_type.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_dict_type.type_code is '字典类型编码';
comment on column sys_dict_type.type_name is '字典类型名称';
comment on column sys_dict_type.built_in is '是否系统内置';
comment on column sys_dict_type.enabled is '是否启用';
comment on column sys_dict_type.remark is '备注';

create unique index if not exists uk_sys_dict_type_code
    on sys_dict_type (type_code);

/*==============================================================*/
/* table: sys_dict_data                                         */
/*==============================================================*/
create table if not exists sys_dict_data
(
    id           bigint not null,
    create_time  timestamp(6),
    create_by    bigint,
    update_time  timestamp(6),
    update_by    bigint,
    data_status  int,
    del_flag     smallint not null default 0,
    dict_type_id bigint not null references sys_dict_type (id),
    dict_code    varchar(100) not null,
    dict_label   varchar(100) not null,
    dict_value   varchar(200) not null,
    sort         int not null default 0,
    built_in     boolean not null default false,
    enabled      boolean not null default true,
    remark       varchar(500),
    primary key (id)
);

comment on table sys_dict_data is '系统字典数据表';
comment on column sys_dict_data.id is '主键id';
comment on column sys_dict_data.create_time is '创建时间';
comment on column sys_dict_data.create_by is '创建人';
comment on column sys_dict_data.update_time is '修改时间';
comment on column sys_dict_data.update_by is '修改人';
comment on column sys_dict_data.data_status is '数据状态';
comment on column sys_dict_data.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_dict_data.dict_type_id is '字典类型id';
comment on column sys_dict_data.dict_code is '字典项编码';
comment on column sys_dict_data.dict_label is '字典项标签';
comment on column sys_dict_data.dict_value is '字典项值';
comment on column sys_dict_data.sort is '排序';
comment on column sys_dict_data.built_in is '是否系统内置';
comment on column sys_dict_data.enabled is '是否启用';
comment on column sys_dict_data.remark is '备注';

create unique index if not exists uk_sys_dict_data_type_code
    on sys_dict_data (dict_type_id, dict_code);

create unique index if not exists uk_sys_dict_data_type_value
    on sys_dict_data (dict_type_id, dict_value);

create index if not exists idx_sys_dict_data_type_enabled_sort
    on sys_dict_data (dict_type_id, enabled, sort, id);

/*==============================================================*/
/* table: sys_organization                                      */
/*==============================================================*/
create table if not exists sys_organization
(
    id             bigint not null,
    create_time    timestamp(6),
    create_by      bigint,
    update_time    timestamp(6),
    update_by      bigint,
    data_status    int,
    del_flag       smallint not null default 0,
    tenant_id      bigint not null,
    parent_id      bigint,
    parent_id_path varchar(500),
    org_code       varchar(100) not null,
    org_name       varchar(100) not null,
    org_type       varchar(50),
    leader_user_id bigint,
    sort           int,
    enabled        boolean default true,
    primary key (id)
);

comment on table sys_organization is '系统组织架构表';
comment on column sys_organization.id is '主键id';
comment on column sys_organization.create_time is '创建时间';
comment on column sys_organization.create_by is '创建人';
comment on column sys_organization.update_time is '修改时间';
comment on column sys_organization.update_by is '修改人';
comment on column sys_organization.data_status is '数据状态';
comment on column sys_organization.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_organization.tenant_id is '租户id';
comment on column sys_organization.parent_id is '父级组织id';
comment on column sys_organization.parent_id_path is '父级组织id路径';
comment on column sys_organization.org_code is '组织编码';
comment on column sys_organization.org_name is '组织名称';
comment on column sys_organization.org_type is '组织类型';
comment on column sys_organization.leader_user_id is '负责人id';
comment on column sys_organization.sort is '排序';
comment on column sys_organization.enabled is '是否启用';

create index if not exists idx_sys_organization_tenant_parent
    on sys_organization (tenant_id, parent_id);

create unique index if not exists uk_sys_organization_tenant_code
    on sys_organization (tenant_id, org_code)
    where del_flag = 0;

/*==============================================================*/
/* table: sys_config                                            */
/*==============================================================*/
create table if not exists sys_config
(
    id              bigint not null,
    create_time     timestamp(6),
    create_by       bigint,
    update_time     timestamp(6),
    update_by       bigint,
    data_status     int,
    del_flag        smallint not null default 0,
    tenant_id       bigint not null,
    parameter_key   varchar(100) not null,
    parameter_name  varchar(100) not null,
    parameter_value text,
    value_type      varchar(50),
    built_in        boolean default false,
    enabled         boolean default true,
    remark          varchar(500),
    primary key (id)
);

comment on table sys_config is '系统参数表';
comment on column sys_config.id is '主键id';
comment on column sys_config.create_time is '创建时间';
comment on column sys_config.create_by is '创建人';
comment on column sys_config.update_time is '修改时间';
comment on column sys_config.update_by is '修改人';
comment on column sys_config.data_status is '数据状态';
comment on column sys_config.del_flag is '逻辑删除标识：0未删除，1已删除';
comment on column sys_config.tenant_id is '租户id';
comment on column sys_config.parameter_key is '参数键';
comment on column sys_config.parameter_name is '参数名称';
comment on column sys_config.parameter_value is '参数值';
comment on column sys_config.value_type is '参数值类型';
comment on column sys_config.built_in is '是否系统内置';
comment on column sys_config.enabled is '是否启用';
comment on column sys_config.remark is '备注';

create index if not exists idx_sys_config_tenant_key
    on sys_config (tenant_id, parameter_key);

create unique index if not exists uk_sys_config_tenant_key
    on sys_config (tenant_id, parameter_key)
    where del_flag = 0;
