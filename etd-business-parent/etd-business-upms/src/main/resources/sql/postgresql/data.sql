/*==============================================================*/
/* sys_tenant: 系统租户表初始化                                  */
/*==============================================================*/
insert into sys_tenant (id, create_time, data_status, parent_id, logo, tenant_name, description, credit_code, tenant_type,
                        tenant_admin_user, parent_id_path, locked, enabled, menus)
values (1, '2024-04-11 16:38:46.000000', 1, null, null, '易开发演示平台', 'easy to develop framework functional demo', '1',
        'system', 1, '1', false, true, '1000001,1000002,1000003,1000004,1000005,1000006,1000007,1000008,1000009')
on conflict (id) do nothing;

insert into sys_tenant (id, create_time, data_status, parent_id, logo, tenant_name, description, credit_code, tenant_type,
                        tenant_admin_user, parent_id_path, locked, enabled, menus)
values (2, '2024-04-11 16:38:46.000000', 1, null, null, '租户演示平台', 'easy to develop framework functional demo', '1',
        'ordinary', 2, '1', false, true, '1000001,1000002,1000003,1000004,1000005,1000006,1000007,1000008,1000009')
on conflict (id) do nothing;

/*==============================================================*/
/* sys_user: 系统用户初始化                                      */
/*==============================================================*/
insert into sys_user (id, create_time, data_status, tenant_id, account, mobile, password, user_name, birthday, gender, avatar,
                      nick_name, locked, enabled)
values (1, '2024-04-11 15:05:51.000000', 1, 1, 'admin', '17719540702',
        '{bcrypt}$2a$10$ltbQNkrjY/tcqP8ytOVSOerYvPMxfNjYgfwv.HaslRAqV/jOyYkSS', '牛昌', '1990-02-02', 1, null,
        '淡淡丶奶油味', false, true)
on conflict (id) do nothing;

insert into sys_user (id, create_time, data_status, tenant_id, account, mobile, password, user_name, birthday, gender, avatar,
                      nick_name, locked, enabled)
values (2, '2024-04-11 15:05:51.000000', 1, 2, 'testtenant', '17719540802',
        '{bcrypt}$2a$10$ltbQNkrjY/tcqP8ytOVSOerYvPMxfNjYgfwv.HaslRAqV/jOyYkSS', '牛昌', '1990-02-02', 1, null,
        '测试租户', false, true)
on conflict (id) do nothing;

/*==============================================================*/
/* sys_role: 系统角色初始化                                      */
/*==============================================================*/
insert into sys_role (id, create_time, data_status, tenant_id, built_in, role_name, role_code, role_desc, permission_type)
values (1, '2024-04-12 09:46:50.000000', 1, 1, true, '平台管理员', 'platformadmin', '平台管理员,拥有系统最高权限', 'all')
on conflict (id) do nothing;

insert into sys_role (id, create_time, data_status, tenant_id, built_in, role_name, role_code, role_desc, permission_type)
values (2, '2024-04-12 09:46:50.000000', 1, 2, false, '租户管理员', 'tenantadmin', '租户管理员,拥有租户最高权限', 'all')
on conflict (id) do nothing;

/*==============================================================*/
/* sys_app: 系统应用初始化                                      */
/*==============================================================*/
insert into sys_app (id, create_time, data_status, tenant_id, app_code, app_name, app_type, app_icon, app_home_path,
                     built_in, trusted, enabled, sort)
values (1, '2024-04-19 11:30:00.000000', 1, 1, 'platform', '基础平台', 'platform', 'settingoutlined', '/',
        true, true, true, 1)
on conflict (id) do nothing;

/*==============================================================*/
/* sys_user_role_rel: 系统用户与角色关系初始化                    */
/*==============================================================*/
insert into sys_user_role_rel (id, create_time, data_status, tenant_id, user_id, role_id)
values (1, '2024-04-12 09:48:30.000000', 1, 1, 1, 1)
on conflict (id) do nothing;

insert into sys_user_role_rel (id, create_time, data_status, tenant_id, user_id, role_id)
values (2, '2024-04-12 09:48:30.000000', 1, 2, 2, 2)
on conflict (id) do nothing;

/*==============================================================*/
/* sys_menus: 系统菜单初始化                                     */
/*==============================================================*/
insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_path, menu_router, menu_icon, menu_type, sort)
values (1000001, null, '2026-08-30 04:57:41.475264', 1, '首页', '/dashboard', '/dashboard', 'AppstoreOutlined', 'MENU', 10)
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_path, menu_router, menu_icon, menu_type, sort)
values (1000002, null, '2026-08-30 04:57:41.475264', 1, '租户管理', '/tenants', '/tenants', 'TeamOutlined', 'MENU', 20)
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_path, menu_router, menu_icon, menu_type, sort)
values (1000003, null, '2026-08-30 04:57:41.475264', 1, '用户中心', '/users', '/users', 'UserOutlined', 'MENU', 30)
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_path, menu_router, menu_icon, menu_type, sort)
values (1000004, null, '2026-08-30 04:57:41.475264', 1, '系统管理', '/system', '/system', 'SettingOutlined', 'DIRECTORY', 40)
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_path, menu_router, menu_icon, menu_type, sort)
values (1000005, 1000004, '2026-08-30 04:57:41.475264', 1, '系统字典', '/system/dictionaries', '/system/dictionaries', 'DatabaseOutlined', 'MENU', 10)
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_path, menu_router, menu_icon, menu_type, sort)
values (1000006, 1000004, '2026-08-30 04:57:41.475264', 1, '角色管理', '/system/roles', '/system/roles', 'SafetyCertificateOutlined', 'MENU', 20)
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_path, menu_router, menu_icon, menu_type, sort)
values (1000007, 1000004, '2026-08-30 04:57:41.475264', 1, '菜单管理', '/system/menus', '/system/menus', 'MenuOutlined', 'MENU', 30)
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_path, menu_router, menu_icon, menu_type, sort)
values (1000008, 1000004, '2026-08-30 04:57:41.475264', 1, '部门管理', '/system/departments', '/system/departments', 'ApartmentOutlined', 'MENU', 40)
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_path, menu_router, menu_icon, menu_type, sort)
values (1000009, 1000004, '2026-08-30 04:57:41.475264', 1, '系统参数', '/system/parameters', '/system/parameters', 'SettingOutlined', 'MENU', 50)
on conflict (id) do nothing;
