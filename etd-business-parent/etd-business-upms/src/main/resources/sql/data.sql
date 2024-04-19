/*==============================================================*/
/* SYSTEM_TENANT: 系统租户表初始化                                 */
/*==============================================================*/
INSERT INTO SYSTEM_TENANT (ID, CREATE_TIME, DATA_STATUS, PARENT_ID,LOGO, TENANT_NAME,DESCRIPTION, CREDIT_CODE, TENANT_TYPE,
                           TENANT_ADMIN_USER, PARENT_ID_PATH, LOCKED, ENABLED,MENUS)
VALUES (1, '2024-04-11 16:38:46.000000', 1, null,null, '易开发演示平台','Easy to Develop framework Functional Demo', '1', 'System', 1, '1', 0, 1,'1,2,3,4,5,6,7');

INSERT INTO SYSTEM_TENANT (ID, CREATE_TIME, DATA_STATUS, PARENT_ID,LOGO, TENANT_NAME,DESCRIPTION, CREDIT_CODE, TENANT_TYPE,
                           TENANT_ADMIN_USER, PARENT_ID_PATH, LOCKED, ENABLED,MENUS)
VALUES (2, '2024-04-11 16:38:46.000000', 1, null,null, '租户演示平台','Easy to Develop framework Functional Demo', '1', 'Ordinary', 2, '1', 0, 1,'1,2,3,4,5,6,7');



/*==============================================================*/
/* SYSTEM_USER: 系统用户初始化                                     */
/*==============================================================*/
INSERT INTO SYSTEM_USER (ID, CREATE_TIME, DATA_STATUS, ACCOUNT, MOBILE, PASSWORD, USER_NAME, BIRTHDAY, GENDER, AVATAR,
                         NICK_NAME, LOCKED, ENABLED)
VALUES (1, '2024-04-11 15:05:51.000000', 1, 'admin', '17719540702',
        '{bcrypt}$2a$10$Nf3xdaN421EBNTWyfLEET.ByX5fYz592ZFAWNX10ProeMdKFT52T.', '牛昌', '1990-02-02', 1, null,
        '淡淡丶奶油味', 0, 1);

INSERT INTO SYSTEM_USER (ID, CREATE_TIME, DATA_STATUS, ACCOUNT, MOBILE, PASSWORD, USER_NAME, BIRTHDAY, GENDER, AVATAR,
                         NICK_NAME, LOCKED, ENABLED)
VALUES (2, '2024-04-11 15:05:51.000000', 1, 'TestTenant', '17719540802',
        '{bcrypt}$2a$10$Nf3xdaN421EBNTWyfLEET.ByX5fYz592ZFAWNX10ProeMdKFT52T.', '牛昌', '1990-02-02', 1, null,
        '测试租户', 0, 1);



/*==============================================================*/
/* SYSTEM_ROLE: 系统角色初始化                                     */
/*==============================================================*/
INSERT INTO SYSTEM_ROLE (ID, CREATE_TIME, DATA_STATUS, TENANT_ID,BUILT_IN, ROLE_NAME, ROLE_CODE, ROLE_DESC,PERMISSION_TYPE,MENUS)
VALUES (1, '2024-04-12 09:46:50.000000', 1, 1,1, '平台管理员', 'PlatformAdmin', '平台管理员,拥有系统最高权限','ALL','1,2,3,4,5,6,7');


INSERT INTO SYSTEM_ROLE (ID, CREATE_TIME, DATA_STATUS, TENANT_ID,BUILT_IN, ROLE_NAME, ROLE_CODE, ROLE_DESC,PERMISSION_TYPE,MENUS)
VALUES (2, '2024-04-12 09:46:50.000000', 1, 2,0, '租户管理员', 'TenantAdmin', '租户管理员,拥有租户最高权限','ALL','1,2,3,4,5,6,7');


/*==============================================================*/
/* SYSTEM_USER_ROLE_REL: 系统用户与角色关系初始化                    */
/*==============================================================*/
INSERT INTO SYSTEM_USER_ROLE_REL (ID, CREATE_TIME, DATA_STATUS, TENANT_ID, USER_ID, ROLE_ID)
VALUES (1, '2024-04-12 09:48:30.000000', 1, 1, 1, 1);

INSERT INTO SYSTEM_USER_ROLE_REL (ID, CREATE_TIME, DATA_STATUS, TENANT_ID, USER_ID, ROLE_ID)
VALUES (2, '2024-04-12 09:48:30.000000', 1, 2, 2, 2);









/*==============================================================*/
/* SYSTEM_MENUS: 系统用户与角色关系初始化                            */
/*==============================================================*/
INSERT INTO SYSTEM_MENUS (ID, PARENT_ID, CREATE_TIME, DATA_STATUS, MENU_NAME, MENU_ROUTER, MENU_ICON, MENU_TYPE, SORT, MENU_PATH) VALUES (1, null, '2024-04-19 11:31:19.000000', 1, 'ETD-后端演示平台', '@/views/index.vue', null, 'MENU', 1, '/');
INSERT INTO SYSTEM_MENUS (ID, PARENT_ID, CREATE_TIME, DATA_STATUS, MENU_NAME, MENU_ROUTER, MENU_ICON, MENU_TYPE, SORT, MENU_PATH) VALUES (2, 1, '2024-04-19 11:32:19.000000', 1, '首页', '@/views/home.vue', 'HomeOutlined', 'MENU', 1, '/home');
INSERT INTO SYSTEM_MENUS (ID, PARENT_ID, CREATE_TIME, DATA_STATUS, MENU_NAME, MENU_ROUTER, MENU_ICON, MENU_TYPE, SORT, MENU_PATH) VALUES (3, 1, '2024-04-19 11:33:26.000000', 1, '系统管理', null, 'SettingOutlined', 'MENU', 2, '/system');
INSERT INTO SYSTEM_MENUS (ID, PARENT_ID, CREATE_TIME, DATA_STATUS, MENU_NAME, MENU_ROUTER, MENU_ICON, MENU_TYPE, SORT, MENU_PATH) VALUES (4, 3, '2024-04-19 11:34:15.000000', 1, '租户管理', '@/views/tenant/index.vue', null, 'MENU', 1, '/system/tenant');
INSERT INTO SYSTEM_MENUS (ID, PARENT_ID, CREATE_TIME, DATA_STATUS, MENU_NAME, MENU_ROUTER, MENU_ICON, MENU_TYPE, SORT, MENU_PATH) VALUES (5, 3, '2024-04-19 11:35:19.000000', 1, '用户管理', '@/views/user/index.vue', 'UserOutlined', 'MENU', 2, '/system/user');
INSERT INTO SYSTEM_MENUS (ID, PARENT_ID, CREATE_TIME, DATA_STATUS, MENU_NAME, MENU_ROUTER, MENU_ICON, MENU_TYPE, SORT, MENU_PATH) VALUES (6, 3, '2024-04-19 11:36:02.000000', 1, '角色管理', '@/views/role/index.vue', null, 'MENU', 3, '/system/role');
INSERT INTO SYSTEM_MENUS (ID, PARENT_ID, CREATE_TIME, DATA_STATUS, MENU_NAME, MENU_ROUTER, MENU_ICON, MENU_TYPE, SORT, MENU_PATH) VALUES (7, 3, '2024-04-19 11:36:43.000000', 1, '部门管理', '@/views/department/index.vue', null, 'MENU', 4, '/system/department');
