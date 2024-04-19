/*==============================================================*/
/* SYSTEM_TENANT: 系统租户表初始化                                 */
/*==============================================================*/
INSERT INTO SYSTEM_TENANT (ID, CREATE_TIME, DATA_STATUS, PARENT_ID,LOGO, TENANT_NAME,DESCRIPTION, CREDIT_CODE, TENANT_TYPE,
                           TENANT_ADMIN_USER, PARENT_ID_PATH, LOCKED, ENABLED,MENUS)
VALUES (1, '2024-04-11 16:38:46.000000', 1, null,null, '易开发演示平台','Easy to Develop framework Functional Demo', '1', 'System', 1, '1', 0, 1,null);


/*==============================================================*/
/* SYSTEM_USER: 系统用户初始化                                     */
/*==============================================================*/
INSERT INTO SYSTEM_USER (ID, CREATE_TIME, DATA_STATUS, ACCOUNT, MOBILE, PASSWORD, USER_NAME, BIRTHDAY, GENDER, AVATAR,
                         NICK_NAME, LOCKED, ENABLED)
VALUES (1, '2024-04-11 15:05:51.000000', 1, 'admin', '17719540702',
        '{bcrypt}$2a$10$Nf3xdaN421EBNTWyfLEET.ByX5fYz592ZFAWNX10ProeMdKFT52T.', '牛昌', '1990-02-02', 1, null,
        '淡淡丶奶油味', 0, 1);


/*==============================================================*/
/* SYSTEM_USER: 系统角色初始化                                     */
/*==============================================================*/
INSERT INTO SYSTEM_ROLE (ID, CREATE_TIME, DATA_STATUS, TENANT_ID,BUILT_IN, ROLE_NAME, ROLE_CODE, ROLE_DESC,PERMISSION_TYPE,MENUS)
VALUES (1, '2024-04-12 09:46:50.000000', 1, 1,1, '平台管理员', 'PlatformAdmin', '平台管理员,拥有系统最高权限','ALL',null);


/*==============================================================*/
/* SYSTEM_USER: 系统用户与角色关系初始化                            */
/*==============================================================*/
INSERT INTO SYSTEM_USER_ROLE_REL (ID, CREATE_TIME, DATA_STATUS, TENANT_ID, USER_ID, ROLE_ID)
VALUES (1, '2024-04-12 09:48:30.000000', 1, 1, 1, 1);
