/*==============================================================*/
/* sys_tenant: 系统租户表初始化                                  */
/*==============================================================*/
insert into sys_tenant (id, create_time, create_by, update_time, update_by, data_status, logo, tenant_name,
                        description, credit_code, tenant_type, tenant_admin_user, locked)
values (1, '2024-04-11 16:38:46.000000+08:00', 1, '2024-04-11 16:38:46.000000+08:00', 1, 1, null,
        'ETD Console', 'easy to develop framework functional demo', '1', 'system', 1, false)
on conflict (id) do nothing;

/*==============================================================*/
/* sys_user: 系统用户初始化                                      */
/*==============================================================*/
insert into sys_user (id, create_time, create_by, update_time, update_by, data_status, tenant_id, account, mobile,
                      password, user_name, birthday, gender, avatar, nick_name, locked, enabled)
values (1, '2024-04-11 15:05:51.000000+08:00', 1, '2024-04-11 15:05:51.000000+08:00', 1, 1, 1, 'admin', '17719540702',
        '{bcrypt}$2a$10$ltbQNkrjY/tcqP8ytOVSOerYvPMxfNjYgfwv.HaslRAqV/jOyYkSS', '牛昌', '1990-02-02', 1, null,
        '淡淡丶奶油味', false, true)
on conflict (id) do nothing;

/*==============================================================*/
/* sys_role: 系统角色初始化                                      */
/*==============================================================*/
insert into sys_role (id, create_time, create_by, update_time, update_by, data_status, tenant_id, built_in,
                      role_name, role_code, role_desc, permission_type)
values (1, '2024-04-12 09:46:50.000000+08:00', 1, '2024-04-12 09:46:50.000000+08:00', 1, 1, 1, true,
        '平台管理员', 'platformAdmin', '平台管理员，拥有系统最高权限', '1')
on conflict (id) do nothing;

/*==============================================================*/
/* sys_user_role_rel: 系统用户与角色关系初始化                    */
/*==============================================================*/
insert into sys_user_role_rel (id, create_time, create_by, update_time, update_by, data_status, tenant_id,
                               user_id, role_id)
values (1, '2024-04-12 09:48:30.000000+08:00', 1, '2024-04-12 09:48:30.000000+08:00', 1, 1, 1, 1, 1)
on conflict (id) do nothing;

/*==============================================================*/
/* sys_menus: 系统菜单初始化                                     */
/*==============================================================*/
insert into sys_menus (id, parent_id, create_time, create_by, update_time, update_by, data_status, menu_name,
                       menu_path, menu_router, menu_icon, menu_type, sort)
values (1000001, null, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '首页', '/dashboard', '@/views/404.vue', 'AppstoreOutlined', 'MENU', 10),
       (1000002, null, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '租户管理', '/system/tenants', '@/views/tenant/index.vue', 'TeamOutlined', 'MENU', 20),
       (1000003, null, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '用户中心', '/system/users', '@/views/system/users/index.vue', 'UserOutlined', 'MENU', 30),
       (1000004, null, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '系统管理', '/system', null, 'SettingOutlined', 'DIRECTORY', 40),
       (1000005, 1000004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '系统字典', '/system/dictionaries', '@/views/system/dictionaries/index.vue', 'DatabaseOutlined', 'MENU', 10),
       (1000006, 1000004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '角色管理', '/system/roles', '@/views/system/roles/index.vue', 'SafetyCertificateOutlined', 'MENU', 20),
       (1000007, 1000004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '菜单管理', '/system/menus', '@/views/system/menus/index.vue', 'MenuOutlined', 'MENU', 30),
       (1000008, 1000004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '组织架构', '/system/organizations', '@/views/system/organizations/index.vue', 'ApartmentOutlined', 'MENU', 40),
       (1000009, 1000004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '系统配置', '/system/config', '@/views/system/config/index.vue', 'SettingOutlined', 'MENU', 50)
on conflict (id) do update
set menu_router = excluded.menu_router;

/*==============================================================*/
/* sys_tenant_menu_rel: 默认租户菜单权限初始化                   */
/*==============================================================*/
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

/*==============================================================*/
/* sys_dict_type: 系统字典类型初始化                              */
/*==============================================================*/
insert into sys_dict_type (id, create_time, create_by, update_time, update_by, data_status, del_flag,
                           type_code, type_name, built_in, enabled, remark)
values (2200001, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'common_status', '启用状态', true, true, '通用启用状态'),
       (2200002, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'common_builtin', '内置标识', true, true, '通用内置标识'),
       (2200003, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'system_config_value_type', '参数值类型', true, true, '系统参数值类型'),
       (2200004, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'system_role_permission_type', '角色数据权限', true, true, '系统角色数据权限范围'),
       (2200005, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'system_menu_access_level', '菜单访问级别', true, true, '角色菜单访问级别'),
       (2200006, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'system_menu_type', '菜单类型', true, true, '系统菜单类型'),
       (2200007, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'system_org_type', '组织类型', true, true, '系统组织机构类型'),
       (2200008, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'system_tenant_type', '租户类型', true, true, '系统租户类型')
on conflict (id) do nothing;

/*==============================================================*/
/* sys_dict_data: 系统字典数据初始化                              */
/*==============================================================*/
insert into sys_dict_data (id, create_time, create_by, update_time, update_by, data_status, del_flag,
                           dict_type_id, dict_code, dict_label, dict_value, sort, built_in, enabled, remark)
values (2300001, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200001, 'ENABLED', '启用', '1', 10, true, true, null),
       (2300002, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200001, 'DISABLED', '禁用', '0', 20, true, true, null),
       (2300003, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200002, 'BUILT_IN', '内置', 'true', 10, true, true, null),
       (2300004, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200002, 'CUSTOM', '自定义', 'false', 20, true, true, null),
       (2300005, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200003, 'STRING', '字符串', 'string', 10, true, true, null),
       (2300006, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200003, 'NUMBER', '数字', 'number', 20, true, true, null),
       (2300007, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200003, 'BOOLEAN', '布尔值', 'boolean', 30, true, true, null),
       (2300008, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200003, 'JSON', 'JSON', 'json', 40, true, true, null),
       (2300009, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200004, 'UNLIMITED', '不限制', '1', 10, true, true, null),
       (2300010, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200004, 'SELF', '仅本人', '2', 20, true, true, null),
       (2300011, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200004, 'CURRENT_ORG', '仅当前组织', '3', 30, true, true, null),
       (2300012, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200004, 'CURRENT_AND_CHILD_ORG', '当前组织及下级组织', '4', 40, true, true, null),
       (2300013, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200004, 'CUSTOM_ORG', '自定义跨组织', '5', 50, true, true, null),
       (2300014, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200005, 'READ_ONLY', '只读', 'READ_ONLY', 10, true, true, null),
       (2300015, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200005, 'READ_WRITE', '读写', 'READ_WRITE', 20, true, true, null),
       (2300016, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200006, 'DIRECTORY', '目录', 'DIRECTORY', 10, true, true, null),
       (2300017, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200006, 'MENU', '菜单', 'MENU', 20, true, true, null),
       (2300018, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200007, 'COMPANY', '公司', 'COMPANY', 10, true, true, null),
       (2300019, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200007, 'DEPARTMENT', '部门', 'DEPARTMENT', 20, true, true, null),
       (2300020, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200007, 'TEAM', '组', 'TEAM', 20, true, true, null),
       (2300021, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200008, 'SYSTEM', '系统租户', 'system', 10, true, true, null),
       (2300022, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200008, 'ORDINARY', '普通租户', 'ordinary', 20, true, true, null)
    on conflict (id) do nothing;



/*==============================================================*/
/* sys_organization: 系统组织架构初始化                            */
/*==============================================================*/
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

/*==============================================================*/
/* sys_config: 系统参数初始化                                    */
/*==============================================================*/
insert into sys_config (id, create_time, create_by, update_time, update_by, data_status, parameter_key,
                        parameter_name, parameter_value, value_type, built_in, enabled, remark)
values (4000001, current_timestamp, 1, current_timestamp, 1, 1,
        'system.branding', '系统视觉与版权信息',
        '{"name":"ETD 后台管理系统","logo":"/assets/images/logo.png","favicon":"/favicon.ico","copyright":"Copyright © 2026 ETD. All Rights Reserved.","watermark":{"enabled":false,"opacity":0.15,"fontSize":14}}',
        'json', true, true, '全局品牌名、Logo链接、水印配置'),
       (4000002, current_timestamp, 1, current_timestamp, 1, 1,
        'security.policy', '登录验证码与密码策略',
        '{"captcha":{"triggerOnFailCount":5},"password":{"minLength":8,"regexp":"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$","defaultPassword":"Etd@123456"}}',
        'json', true, true, '纯前端登录失败验证码控制、密码正则及默认初始密码'),
       (4000003, current_timestamp, 1, current_timestamp, 1, 1,
        'system.resource.limit', '业务资源与树形层级限制',
        '{"upload":{"maxSizeMb":10,"allowedExtensions":[".png",".jpg",".jpeg",".pdf",".xlsx",".zip",".docx"]},"organization":{"maxDepth":6,"exportLimit":5000},"menu":{"maxDepth":4},"pagination":{"defaultSize":10,"maxSize":200}}',
        'json', true, true, '上传大小限制、组织树和菜单树的最大深度控制、分页限制'),
       (4000004, current_timestamp, 1, current_timestamp, 1, 1,
        'system.network.policy', '网络通信与缓存策略',
        '{"request":{"timeoutMs":5000,"retryTimes":3},"cache":{"dictTtlSeconds":600,"userMenuTtlSeconds":1800}}',
        'json', true, true, '前端超时时间、请求重试次数及字典等数据缓存时效')
on conflict (id) do nothing;
