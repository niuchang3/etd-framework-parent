create table if not exists system_tenant
(
    id                bigint primary key,
    create_time       timestamp,
    data_status       boolean,
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
    menus             text
);

comment on table system_tenant is '系统租户表';
comment on column system_tenant.id is '主键ID';
comment on column system_tenant.create_time is '创建时间';
comment on column system_tenant.data_status is '数据状态';
comment on column system_tenant.parent_id is '父级租户ID';
comment on column system_tenant.logo is 'Logo地址';
comment on column system_tenant.tenant_name is '租户名称';
comment on column system_tenant.description is '租户描述';
comment on column system_tenant.credit_code is '信用代码';
comment on column system_tenant.tenant_type is '企业类型';
comment on column system_tenant.tenant_admin_user is '租户管理员';
comment on column system_tenant.parent_id_path is '租户ID_PATH';
comment on column system_tenant.locked is '租户是否锁定';
comment on column system_tenant.enabled is '租户启用';
comment on column system_tenant.menus is '租户菜单';

create table if not exists "system_user"
(
    id          bigint primary key,
    create_time timestamp,
    data_status boolean,
    account     varchar(32),
    mobile      varchar(20),
    password    varchar(100),
    user_name   varchar(20),
    birthday    date,
    gender      integer,
    avatar      varchar(200),
    nick_name   varchar(100),
    locked      boolean,
    enabled     boolean
);

comment on table "system_user" is '系统用户表';
comment on column "system_user".id is '主键ID';
comment on column "system_user".create_time is '创建时间';
comment on column "system_user".data_status is '数据状态';
comment on column "system_user".account is '账号';
comment on column "system_user".mobile is '手机号码';
comment on column "system_user".password is '密码';
comment on column "system_user".user_name is '用户姓名';
comment on column "system_user".birthday is '生日';
comment on column "system_user".gender is '性别';
comment on column "system_user".avatar is '用户头像';
comment on column "system_user".nick_name is '用户昵称';
comment on column "system_user".locked is '账号锁定';
comment on column "system_user".enabled is '账号启用';

create unique index if not exists uk_system_user_account on "system_user" (account);

create table if not exists system_role
(
    id              bigint primary key,
    create_time     timestamp,
    data_status     boolean,
    tenant_id       bigint,
    built_in        boolean,
    role_name       varchar(20),
    role_code       varchar(50),
    role_desc       varchar(200),
    permission_type varchar(30),
    menus           text
);

comment on table system_role is '系统角色表';
comment on column system_role.id is '主键ID';
comment on column system_role.create_time is '创建时间';
comment on column system_role.data_status is '数据状态';
comment on column system_role.tenant_id is '租户ID';
comment on column system_role.built_in is '是否内置';
comment on column system_role.role_name is '角色名称';
comment on column system_role.role_code is '角色CODE';
comment on column system_role.role_desc is '角色描述';
comment on column system_role.permission_type is '权限类型';
comment on column system_role.menus is '角色菜单';

create table if not exists system_user_role_rel
(
    id          bigint primary key,
    create_time timestamp,
    data_status boolean,
    tenant_id   varchar(32),
    user_id     bigint,
    role_id     bigint
);

comment on table system_user_role_rel is '系统用户与角色的关系表';
comment on column system_user_role_rel.id is '主键ID';
comment on column system_user_role_rel.create_time is '创建时间';
comment on column system_user_role_rel.data_status is '数据状态';
comment on column system_user_role_rel.tenant_id is '租户ID';
comment on column system_user_role_rel.user_id is '用户ID';
comment on column system_user_role_rel.role_id is '角色ID';

create table if not exists system_menus
(
    id          bigint primary key,
    parent_id   bigint,
    create_time timestamp,
    data_status boolean,
    menu_name   varchar(20),
    menu_path   varchar(100),
    menu_router varchar(100),
    menu_icon   varchar(200),
    menu_type   varchar(20),
    sort        integer
);

comment on table system_menus is '系统菜单';
comment on column system_menus.id is '主键ID';
comment on column system_menus.parent_id is '父级菜单';
comment on column system_menus.create_time is '创建时间';
comment on column system_menus.data_status is '数据状态';
comment on column system_menus.menu_name is '菜单名称';
comment on column system_menus.menu_path is '菜单PATH';
comment on column system_menus.menu_router is '菜单路由';
comment on column system_menus.menu_icon is '菜单图标';
comment on column system_menus.menu_type is '菜单类型';
comment on column system_menus.sort is '排序';
