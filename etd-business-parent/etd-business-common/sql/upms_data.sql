-- UPMS专属基础数据：默认租户、管理员、角色、组织及默认租户菜单授权。
-- 先执行upms.sql、init.sql；账号和租户授权沿用原有初始化数据。
begin;

-- sys_tenant 初始化。
insert into sys_tenant (id, create_time, create_by, update_time, update_by, data_status, logo, tenant_name,
                        description, credit_code, tenant_type, tenant_admin_user, locked)
values (1, '2024-04-11 16:38:46.000000+08:00', 1, '2024-04-11 16:38:46.000000+08:00', 1, 1, null,
        'ETD Console', 'easy to develop framework functional demo', '1', 'system', 1, false)
on conflict (id) do nothing;

-- sys_user 初始化。
insert into sys_user (id, create_time, create_by, update_time, update_by, data_status, tenant_id, account, mobile,
                      password, user_name, birthday, gender, avatar, nick_name, locked, enabled)
values (1, '2024-04-11 15:05:51.000000+08:00', 1, '2024-04-11 15:05:51.000000+08:00', 1, 1, 1, 'admin', '17719540702',
        '{bcrypt}$2a$10$ltbQNkrjY/tcqP8ytOVSOerYvPMxfNjYgfwv.HaslRAqV/jOyYkSS', '牛昌', '1990-02-02', 1, null,
        '淡淡丶奶油味', false, true)
on conflict (id) do nothing;

-- sys_role 初始化。
insert into sys_role (id, create_time, create_by, update_time, update_by, data_status, tenant_id, built_in,
                      role_name, role_code, role_desc, permission_type)
values (1, '2024-04-12 09:46:50.000000+08:00', 1, '2024-04-12 09:46:50.000000+08:00', 1, 1, 1, true,
        '平台管理员', 'PLATFORM_ADMIN', '平台管理员，拥有系统最高权限', '1')
on conflict (id) do nothing;

-- sys_user_role_rel 初始化。
insert into sys_user_role_rel (id, create_time, create_by, update_time, update_by, data_status, tenant_id,
                               user_id, role_id)
values (1, '2024-04-12 09:48:30.000000+08:00', 1, '2024-04-12 09:48:30.000000+08:00', 1, 1, 1, 1, 1)
on conflict (id) do nothing;

-- sys_tenant_menu_rel 初始化。
insert into sys_tenant_menu_rel (id, create_time, create_by, update_time, update_by, data_status, tenant_id, menu_id)
values (1100001, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1, 1, 1000001),
       (1100002, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1, 1, 1000002),
       (1100003, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1, 1, 1000003),
       (1100004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1, 1, 1000004),
       (1100005, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1, 1, 1000005),
       (1100006, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1, 1, 1000006),
       (1100007, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1, 1, 1000007),
       (1100008, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1, 1, 1000008),
       (1100009, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1, 1, 1000009)
on conflict (id) do nothing;

-- sys_organization 初始化。
INSERT INTO sys_organization (
    id, create_time, create_by, update_time, update_by, data_status, del_flag, tenant_id, parent_id, parent_id_path, org_code, org_name, org_type, leader_user_id, sort, enabled
) VALUES
      (2094438638732578818, '2026-08-31 22:54:18.939000+08:00', 1, '2026-08-31 22:54:18.946000+08:00', 1, 1, 0, 1, NULL, '/', 'YSPT', '演示平台', 'COMPANY', NULL, 1, true),
      (2094438716536918017, '2026-08-31 22:54:37.470000+08:00', 1, '2026-08-31 22:54:37.470000+08:00', 1, 1, 0, 1, 2094438638732578818, '/2094438638732578818/', 'JSYFB', '技术研发部', 'DEPARTMENT', NULL, 2, true),
      (2094438814792683522, '2026-08-31 22:55:00.896000+08:00', 1, '2026-08-31 22:55:00.896000+08:00', 1, 1, 0, 1, 2094438638732578818, '/2094438638732578818/', 'CPYFB', '产品研发部', 'DEPARTMENT', NULL, 3, true),
      (2094438920245874690, '2026-08-31 22:55:26.038000+08:00', 1, '2026-08-31 22:55:26.039000+08:00', 1, 1, 0, 1, 2094438638732578818, '/2094438638732578818/', 'XSB', '销售部', 'DEPARTMENT', NULL, 4, true),
      (2094439019298557954, '2026-08-31 22:55:49.653000+08:00', 1, '2026-08-31 22:55:49.654000+08:00', 1, 1, 0, 1, 2094438638732578818, '/2094438638732578818/', 'XZB', '行政部', 'DEPARTMENT', NULL, 5, true),
      (2094439206968496129, '2026-08-31 22:56:34.398000+08:00', 1, '2026-08-31 22:56:34.398000+08:00', 1, 1, 0, 1, 2094438638732578818, '/2094438638732578818/', 'YYB', '运营部', 'DEPARTMENT', NULL, 7, true),
      (2094439089842556930, '2026-08-31 22:56:06.473000+08:00', 1, '2026-08-31 22:57:31.182000+08:00', 1, 1, 0, 1, 2094438638732578818, '/2094438638732578818/', 'RSB', '人事部', 'DEPARTMENT', NULL, 8, true);

-- 默认租户 AI 菜单授权，包含 AI 管理父菜单和知识库入口。
insert into sys_tenant_menu_rel (id, create_time, create_by, update_time, update_by, data_status, del_flag,
                                 tenant_id, menu_id)
values (1100101, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000101),
       (1100102, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000102),
       (1100106, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000106),
       (1100105, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000105),
       (1100107, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000107);

-- sys_tenant_menu_rel 初始化。
insert into sys_tenant_menu_rel (id, create_time, create_by, update_time, update_by, data_status, tenant_id, menu_id)
values (1100201, current_timestamp, 1, current_timestamp, 1, 1, 1, 1000010),
       (1100202, current_timestamp, 1, current_timestamp, 1, 1, 1, 1000011),
       (1100203, current_timestamp, 1, current_timestamp, 1, 1, 1, 1000012),
       (1100204, current_timestamp, 1, current_timestamp, 1, 1, 1, 1000013),
       (1100205, current_timestamp, 1, current_timestamp, 1, 1, 1, 1000014)
on conflict (id) do nothing;

commit;
