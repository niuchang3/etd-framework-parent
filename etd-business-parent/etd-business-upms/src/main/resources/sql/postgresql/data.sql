/*==============================================================*/
/* sys_tenant: 系统租户表初始化                                  */
/*==============================================================*/
insert into sys_tenant (id, create_time, data_status, parent_id, logo, tenant_name, description, credit_code, tenant_type,
                        tenant_admin_user, parent_id_path, locked, enabled, menus)
values (1, '2024-04-11 16:38:46.000000', 1, null, null, '易开发演示平台', 'easy to develop framework functional demo', '1',
        'system', 1, '1', false, true, '1,2,3,4,5,6,7')
on conflict (id) do nothing;

insert into sys_tenant (id, create_time, data_status, parent_id, logo, tenant_name, description, credit_code, tenant_type,
                        tenant_admin_user, parent_id_path, locked, enabled, menus)
values (2, '2024-04-11 16:38:46.000000', 1, null, null, '租户演示平台', 'easy to develop framework functional demo', '1',
        'ordinary', 2, '1', false, true, '1,2,3,4,5,6,7')
on conflict (id) do nothing;

/*==============================================================*/
/* sys_user: 系统用户初始化                                      */
/*==============================================================*/
insert into sys_user (id, create_time, data_status, account, mobile, password, user_name, birthday, gender, avatar,
                      nick_name, locked, enabled)
values (1, '2024-04-11 15:05:51.000000', 1, 'admin', '17719540702',
        '{bcrypt}$2a$10$t0nmxVeITKheVbLw9OvjC.sIYh62XpRP78NaB3mEiqKOdFa0FcC5S', '牛昌', '1990-02-02', 1, null,
        '淡淡丶奶油味', false, true)
on conflict (id) do nothing;

insert into sys_user (id, create_time, data_status, account, mobile, password, user_name, birthday, gender, avatar,
                      nick_name, locked, enabled)
values (2, '2024-04-11 15:05:51.000000', 1, 'testtenant', '17719540802',
        '{bcrypt}$2a$10$t0nmxVeITKheVbLw9OvjC.sIYh62XpRP78NaB3mEiqKOdFa0FcC5S', '牛昌', '1990-02-02', 1, null,
        '测试租户', false, true)
on conflict (id) do nothing;

/*==============================================================*/
/* sys_role: 系统角色初始化                                      */
/*==============================================================*/
insert into sys_role (id, create_time, data_status, tenant_id, built_in, role_name, role_code, role_desc, permission_type, menus)
values (1, '2024-04-12 09:46:50.000000', 1, 1, true, '平台管理员', 'platformadmin', '平台管理员,拥有系统最高权限', 'all',
        '1,2,3,4,5,6,7')
on conflict (id) do nothing;

insert into sys_role (id, create_time, data_status, tenant_id, built_in, role_name, role_code, role_desc, permission_type, menus)
values (2, '2024-04-12 09:46:50.000000', 1, 2, false, '租户管理员', 'tenantadmin', '租户管理员,拥有租户最高权限', 'all',
        '1,2,3,4,5,6,7')
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
insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_router, menu_icon, menu_type, sort, menu_path)
values (1, null, '2024-04-19 11:31:19.000000', 1, 'etd-后端演示平台', '@/views/index.vue', null, 'menu', 1, '/')
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_router, menu_icon, menu_type, sort, menu_path)
values (2, 1, '2024-04-19 11:32:19.000000', 1, '首页', '@/views/home.vue', 'homeoutlined', 'menu', 1, '/home')
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_router, menu_icon, menu_type, sort, menu_path)
values (3, 1, '2024-04-19 11:33:26.000000', 1, '系统管理', null, 'settingoutlined', 'menu', 2, '/system')
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_router, menu_icon, menu_type, sort, menu_path)
values (4, 3, '2024-04-19 11:34:15.000000', 1, '租户管理', '@/views/tenant/index.vue', null, 'menu', 1, '/system/tenant')
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_router, menu_icon, menu_type, sort, menu_path)
values (5, 3, '2024-04-19 11:35:19.000000', 1, '用户管理', '@/views/user/index.vue', 'useroutlined', 'menu', 2, '/system/user')
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_router, menu_icon, menu_type, sort, menu_path)
values (6, 3, '2024-04-19 11:36:02.000000', 1, '角色管理', '@/views/role/index.vue', null, 'menu', 3, '/system/role')
on conflict (id) do nothing;

insert into sys_menus (id, parent_id, create_time, data_status, menu_name, menu_router, menu_icon, menu_type, sort, menu_path)
values (7, 3, '2024-04-19 11:36:43.000000', 1, '部门管理', '@/views/department/index.vue', null, 'menu', 4, '/system/department')
on conflict (id) do nothing;
