insert into system_tenant
    (id, create_time, data_status, parent_id, logo, tenant_name, description, credit_code, tenant_type,
     tenant_admin_user, parent_id_path, locked, enabled, menus)
values
    (1, '2024-04-11 16:38:46', true, null, null, '易开发演示平台',
     'Easy to Develop framework Functional Demo', '1', 'System', 1, '1', false, true, '1,2,3,4,5,6,7'),
    (2, '2024-04-11 16:38:46', true, null, null, '租户演示平台',
     'Easy to Develop framework Functional Demo', '1', 'Ordinary', 2, '1', false, true, '1,2,3,4,5,6,7')
on conflict (id) do nothing;

insert into "system_user"
    (id, create_time, data_status, account, mobile, password, user_name, birthday, gender, avatar,
     nick_name, locked, enabled)
values
    (1, '2024-04-11 15:05:51', true, 'admin', '17719540702',
     '{bcrypt}$2a$10$Nf3xdaN421EBNTWyfLEET.ByX5fYz592ZFAWNX10ProeMdKFT52T.', '牛昌',
     '1990-02-02', 1, null, '淡淡丶奶油味', false, true),
    (2, '2024-04-11 15:05:51', true, 'TestTenant', '17719540802',
     '{bcrypt}$2a$10$Nf3xdaN421EBNTWyfLEET.ByX5fYz592ZFAWNX10ProeMdKFT52T.', '牛昌',
     '1990-02-02', 1, null, '测试租户', false, true)
on conflict (id) do nothing;

insert into system_role
    (id, create_time, data_status, tenant_id, built_in, role_name, role_code, role_desc, permission_type, menus)
values
    (1, '2024-04-12 09:46:50', true, 1, true, '平台管理员', 'PlatformAdmin',
     '平台管理员,拥有系统最高权限', 'ALL', '1,2,3,4,5,6,7'),
    (2, '2024-04-12 09:46:50', true, 2, false, '租户管理员', 'TenantAdmin',
     '租户管理员,拥有租户最高权限', 'ALL', '1,2,3,4,5,6,7')
on conflict (id) do nothing;

insert into system_user_role_rel
    (id, create_time, data_status, tenant_id, user_id, role_id)
values
    (1, '2024-04-12 09:48:30', true, '1', 1, 1),
    (2, '2024-04-12 09:48:30', true, '2', 2, 2)
on conflict (id) do nothing;

insert into system_menus
    (id, parent_id, create_time, data_status, menu_name, menu_router, menu_icon, menu_type, sort, menu_path)
values
    (1, null, '2024-04-19 11:31:19', true, 'ETD-后端演示平台', '@/views/index.vue', null, 'MENU', 1, '/'),
    (2, 1, '2024-04-19 11:32:19', true, '首页', '@/views/home.vue', 'HomeOutlined', 'MENU', 1, '/home'),
    (3, 1, '2024-04-19 11:33:26', true, '系统管理', null, 'SettingOutlined', 'MENU', 2, '/system'),
    (4, 3, '2024-04-19 11:34:15', true, '租户管理', '@/views/tenant/index.vue', null, 'MENU', 1, '/system/tenant'),
    (5, 3, '2024-04-19 11:35:19', true, '用户管理', '@/views/user/index.vue', 'UserOutlined', 'MENU', 2, '/system/user'),
    (6, 3, '2024-04-19 11:36:02', true, '角色管理', '@/views/role/index.vue', null, 'MENU', 3, '/system/role'),
    (7, 3, '2024-04-19 11:36:43', true, '部门管理', '@/views/department/index.vue', null, 'MENU', 4, '/system/department')
on conflict (id) do nothing;
