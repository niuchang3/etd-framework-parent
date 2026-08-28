
/*==============================================================*/
/* table: sys_tenant                                           */
/*==============================================================*/
create table sys_tenant
(
    id                   bigint(32) not null comment '主键id',
    create_time          datetime comment '创建时间',
    data_status          int comment '数据状态',
    parent_id            bigint(32) comment '父级租户id',
    logo                 varchar(200) comment 'logo地址',
    tenant_name          varchar(100) comment '租户名称',
    description          varchar(200) comment '租户描述',
    credit_code          varchar(100) comment '信用代码',
    tenant_type          varchar(100) comment '企业类型',
    tenant_admin_user    bigint(32) comment '租户管理员',
    parent_id_path       varchar(200) comment '租户id_path',
    locked               tinyint(1) comment '租户是否锁定',
    enabled              tinyint(1) comment '租户启用',
    menus                text        comment '租户菜单',

    primary key (id)
);

-- alter table sys_tenant comment '系统租户表';

/*==============================================================*/
/* table: sys_user                                           */
/*==============================================================*/
create table sys_user
(
    id                   bigint(32) not null comment '主键id',
    create_time          datetime comment '创建时间',
    data_status          int comment '数据状态',
    account              varchar(32) comment '账号',
    mobile               varchar(20) comment '手机号码',
    password             varchar(100) comment '密码',
    user_name            varchar(20) comment '用户姓名',
    birthday             date comment '生日',
    gender               int(1) comment '性别',
    avatar               varchar(200) comment '用户头像',
    nick_name            varchar(100) comment '用户昵称',
    locked               tinyint(1) comment '账号锁定',
    enabled              tinyint(1) comment '账号启用',

    primary key (id)
);

-- alter table sys_user comment '系统用户表';



/*==============================================================*/
/* table: sys_role                                           */
/*==============================================================*/
create table sys_role
(
    id                   bigint(32) not null comment '主键id',
    create_time          datetime comment '创建时间',
    data_status          int comment '数据状态',
    tenant_id            bigint(32) comment '租户id',
    built_in             tinyint(1) comment '是否内置',
    role_name            varchar(20) comment '角色名称',
    role_code            varchar(50) comment '角色code',
    role_desc            varchar(200) comment '角色描述',
    permission_type      varchar(30) comment '权限类型',
    menus                text        comment '角色菜单',
    primary key (id)
);

-- alter table sys_role comment '系统角色表';


/*==============================================================*/
/* table: sys_user_role_r                                    */
/*==============================================================*/
create table sys_user_role_rel
(
    id                   bigint(32) not null comment '主键id',
    create_time          datetime comment '创建时间',
    data_status          int comment '数据状态',
    tenant_id            bigint(32) comment '租户id',
    user_id              bigint(32) comment '用户id',
    role_id              bigint(32) comment '角色id',
    primary key (id)
);

-- alter table sys_user_role_r comment '系统用户与角色的关系表';



/*==============================================================*/
/* table: sys_menus                                          */
/*==============================================================*/
create table sys_menus
(
    id                   bigint(32) not null comment '主键id',
    parent_id            bigint(32) comment '父级菜单',
    create_time          datetime comment '创建时间',
    data_status          int comment '数据状态',
    menu_name            varchar(10) comment '菜单名称',
    menu_path            varchar(100) comment '菜单path',
    menu_router          varchar(100) comment '菜单路由',
    menu_icon            varchar(200) comment '菜单图表',
    menu_type            varchar(20) comment '菜单类型',
    sort                 int(11) comment '排序',
    primary key (id)
);

-- alter table sys_menus comment '系统菜单';
