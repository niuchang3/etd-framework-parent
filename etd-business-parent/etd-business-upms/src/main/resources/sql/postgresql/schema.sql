/*==============================================================*/
/* table: sys_tenant                                            */
/*==============================================================*/
create table if not exists sys_tenant
(
    id                bigint not null,
    create_time       timestamp(6),
    data_status       int,
    parent_id         bigint,
    logo              varchar(200),
    tenant_name       varchar(100),
    description       varchar(200),
    credit_code       varchar(100),
    tenant_type       varchar(100),
    tenant_admin_user bigint,
    parent_id_path    varchar(200),
    locked            boolean,
    enabled           boolean,
    menus             text,
    primary key (id)
);

comment on table sys_tenant is '系统租户表';
comment on column sys_tenant.id is '主键id';
comment on column sys_tenant.create_time is '创建时间';
comment on column sys_tenant.data_status is '数据状态';
comment on column sys_tenant.parent_id is '父级租户id';
comment on column sys_tenant.logo is 'logo地址';
comment on column sys_tenant.tenant_name is '租户名称';
comment on column sys_tenant.description is '租户描述';
comment on column sys_tenant.credit_code is '信用代码';
comment on column sys_tenant.tenant_type is '企业类型';
comment on column sys_tenant.tenant_admin_user is '租户管理员';
comment on column sys_tenant.parent_id_path is '租户id_path';
comment on column sys_tenant.locked is '租户是否锁定';
comment on column sys_tenant.enabled is '租户启用';
comment on column sys_tenant.menus is '租户菜单';

/*==============================================================*/
/* table: sys_user                                              */
/*==============================================================*/
create table if not exists sys_user
(
    id          bigint not null,
    create_time timestamp(6),
    data_status int,
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
comment on column sys_user.data_status is '数据状态';
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
    data_status     int,
    tenant_id       bigint,
    built_in        boolean,
    role_name       varchar(20),
    role_code       varchar(50),
    role_desc       varchar(200),
    permission_type varchar(30),
    menus           text,
    primary key (id)
);

comment on table sys_role is '系统角色表';
comment on column sys_role.id is '主键id';
comment on column sys_role.create_time is '创建时间';
comment on column sys_role.data_status is '数据状态';
comment on column sys_role.tenant_id is '租户id';
comment on column sys_role.built_in is '是否内置';
comment on column sys_role.role_name is '角色名称';
comment on column sys_role.role_code is '角色code';
comment on column sys_role.role_desc is '角色描述';
comment on column sys_role.permission_type is '权限类型';
comment on column sys_role.menus is '角色菜单';

/*==============================================================*/
/* table: sys_app                                               */
/*==============================================================*/
create table if not exists sys_app
(
    id            bigint not null,
    create_time   timestamp(6),
    data_status   int,
    app_code      varchar(100) not null,
    app_name      varchar(100) not null,
    app_type      varchar(50),
    app_icon      varchar(200),
    app_home_path varchar(200),
    built_in      boolean default false,
    trusted       boolean default false,
    enabled       boolean default true,
    sort          int,
    primary key (id)
);

comment on table sys_app is '系统应用表';
comment on column sys_app.id is '主键id';
comment on column sys_app.create_time is '创建时间';
comment on column sys_app.data_status is '数据状态';
comment on column sys_app.app_code is '应用编码';
comment on column sys_app.app_name is '应用名称';
comment on column sys_app.app_type is '应用类型';
comment on column sys_app.app_icon is '应用图标';
comment on column sys_app.app_home_path is '应用首页路径';
comment on column sys_app.built_in is '是否系统内置应用';
comment on column sys_app.trusted is '是否平台可信应用';
comment on column sys_app.enabled is '是否启用';
comment on column sys_app.sort is '排序';

create unique index if not exists uk_sys_app_code
    on sys_app (app_code);

/*==============================================================*/
/* table: sys_tenant_app                                        */
/*==============================================================*/
create table if not exists sys_tenant_app
(
    id           bigint not null,
    create_time  timestamp(6),
    data_status  int,
    tenant_id    bigint not null,
    app_id       bigint not null,
    enabled      boolean default true,
    start_time   timestamp(6),
    expire_time  timestamp(6),
    package_code varchar(100),
    primary key (id)
);

comment on table sys_tenant_app is '租户应用开通关系表';
comment on column sys_tenant_app.id is '主键id';
comment on column sys_tenant_app.create_time is '创建时间';
comment on column sys_tenant_app.data_status is '数据状态';
comment on column sys_tenant_app.tenant_id is '租户id';
comment on column sys_tenant_app.app_id is '应用id';
comment on column sys_tenant_app.enabled is '是否启用';
comment on column sys_tenant_app.start_time is '开通时间';
comment on column sys_tenant_app.expire_time is '过期时间';
comment on column sys_tenant_app.package_code is '套餐编码';

create unique index if not exists uk_sys_tenant_app
    on sys_tenant_app (tenant_id, app_id);

/*==============================================================*/
/* table: sys_user_role_rel                                     */
/*==============================================================*/
create table if not exists sys_user_role_rel
(
    id          bigint not null,
    create_time timestamp(6),
    data_status int,
    tenant_id   bigint,
    user_id     bigint,
    role_id     bigint,
    primary key (id)
);

comment on table sys_user_role_rel is '系统用户与角色的关系表';
comment on column sys_user_role_rel.id is '主键id';
comment on column sys_user_role_rel.create_time is '创建时间';
comment on column sys_user_role_rel.data_status is '数据状态';
comment on column sys_user_role_rel.tenant_id is '租户id';
comment on column sys_user_role_rel.user_id is '用户id';
comment on column sys_user_role_rel.role_id is '角色id';

/*==============================================================*/
/* table: sys_menus                                             */
/*==============================================================*/
create table if not exists sys_menus
(
    id          bigint not null,
    app_id      bigint,
    parent_id   bigint,
    create_time timestamp(6),
    data_status int,
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
comment on column sys_menus.app_id is '所属应用id';
comment on column sys_menus.parent_id is '父级菜单';
comment on column sys_menus.create_time is '创建时间';
comment on column sys_menus.data_status is '数据状态';
comment on column sys_menus.menu_name is '菜单名称';
comment on column sys_menus.menu_path is '菜单path';
comment on column sys_menus.menu_router is '菜单路由';
comment on column sys_menus.menu_icon is '菜单图标';
comment on column sys_menus.menu_type is '菜单类型';
comment on column sys_menus.sort is '排序';

create index if not exists idx_sys_menus_app_id
    on sys_menus (app_id);
