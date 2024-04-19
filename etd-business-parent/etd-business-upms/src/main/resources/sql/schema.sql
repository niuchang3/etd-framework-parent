
/*==============================================================*/
/* Table: system_tenant                                           */
/*==============================================================*/
create table system_tenant
(
    ID                   bigint(32) not null comment '主键ID',
    CREATE_TIME          datetime comment '创建时间',
    DATA_STATUS          tinyint(1) comment '数据状态',
    PARENT_ID            bigint(32) comment '父级租户ID',
    LOGO                 varchar(200) comment 'Logo地址',
    TENANT_NAME          varchar(100) comment '租户名称',
    DESCRIPTION          varchar(200) comment '租户描述',
    CREDIT_CODE          varchar(100) comment '信用代码',
    TENANT_TYPE          varchar(100) comment '企业类型',
    TENANT_ADMIN_USER    bigint(32) comment '租户管理员',
    PARENT_ID_PATH       varchar(200) comment '租户ID_PATH',
    LOCKED               tinyint(1) comment '租户是否锁定',
    ENABLED              tinyint(1) comment '租户启用',
    MENUS                text        comment '租户菜单',

    primary key (ID)
);

-- alter table system_tenant comment '系统租户表';

/*==============================================================*/
/* Table: system_user                                           */
/*==============================================================*/
create table system_user
(
    ID                   bigint(32) not null comment '主键ID',
    CREATE_TIME          datetime comment '创建时间',
    DATA_STATUS          tinyint(1) comment '数据状态',
    ACCOUNT              varchar(32) comment '账号',
    MOBILE               varchar(20) comment '手机号码',
    PASSWORD             varchar(100) comment '密码',
    USER_NAME            varchar(20) comment '用户姓名',
    BIRTHDAY             date comment '生日',
    GENDER               int(1) comment '性别',
    AVATAR               varchar(200) comment '用户头像',
    NICK_NAME            varchar(100) comment '用户昵称',
    LOCKED               tinyint(1) comment '账号锁定',
    ENABLED              tinyint(1) comment '账号启用',

    primary key (ID)
);

-- alter table system_user comment '系统用户表';



/*==============================================================*/
/* Table: system_role                                           */
/*==============================================================*/
create table system_role
(
    ID                   bigint(32) not null comment '主键ID',
    CREATE_TIME          datetime comment '创建时间',
    DATA_STATUS          tinyint(1) comment '数据状态',
    TENANT_ID            bigint(32) comment '租户ID',
    BUILT_IN             tinyint(1) comment '是否内置',
    ROLE_NAME            varchar(20) comment '角色名称',
    ROLE_CODE            varchar(50) comment '角色CODE',
    ROLE_DESC            varchar(200) comment '角色描述',
    PERMISSION_TYPE      varchar(30) comment '权限类型',
    MENUS                text        comment '角色菜单',
    primary key (ID)
);

-- alter table system_role comment '系统角色表';


/*==============================================================*/
/* Table: system_user_role_r                                    */
/*==============================================================*/
create table system_user_role_rel
(
    ID                   bigint(32) not null comment '主键ID',
    CREATE_TIME          datetime comment '创建时间',
    DATA_STATUS          tinyint(1) comment '数据状态',
    TENANT_ID            bigint(32) comment '租户ID',
    USER_ID              bigint(32) comment '用户ID',
    ROLE_ID              bigint(32) comment '角色ID',
    primary key (ID)
);

-- alter table system_user_role_r comment '系统用户与角色的关系表';



/*==============================================================*/
/* Table: system_menus                                          */
/*==============================================================*/
create table system_menus
(
    ID                   bigint(32) not null comment '主键ID',
    PARENT_ID            bigint(32) comment '父级菜单',
    CREATE_TIME          datetime comment '创建时间',
    DATA_STATUS          tinyint(1) comment '数据状态',
    MENU_NAME            varchar(10) comment '菜单名称',
    MENU_PATH          varchar(100) comment '菜单PATH',
    MENU_ROUTER          varchar(100) comment '菜单路由',
    MENU_ICON            varchar(200) comment '菜单图表',
    MENU_TYPE            varchar(20) comment '菜单类型',
    SORT                 int(11) comment '排序',
    primary key (ID)
);

-- alter table system_menus comment '系统菜单';

