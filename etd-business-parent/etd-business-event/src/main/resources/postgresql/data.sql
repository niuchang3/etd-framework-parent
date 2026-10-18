/*==============================================================*/
/* Event 事件中心菜单初始化，由 Event 工程维护。                 */
/* 在 UPMS 系统表所在数据库执行，先完成 UPMS schema.sql、data.sql。 */
/* 本脚本手动执行，不接入 Event 应用启动时的数据库初始化。       */
/* 菜单 ID：1000010～1000014；租户菜单关系 ID：1100201～1100205。 */
/*==============================================================*/
begin;

-- 目录仅组织导航；页面保存资源权限码，框架按授权级别生成 :read、:write 权限。
-- data_status=1 表示启用，未指定的 del_flag 使用表默认值 0（未删除）。
insert into sys_menus (id, parent_id, create_time, create_by, update_time, update_by, data_status, menu_name,
                       menu_path, menu_router, menu_icon, menu_type, sort, permission_code)
values (1000010, null, current_timestamp, 1, current_timestamp, 1, 1,
        '事件中心', '/event', null, 'NotificationOutlined', 'DIRECTORY', 50, null),
       (1000011, 1000010, current_timestamp, 1, current_timestamp, 1, 1,
        '事件类型', '/event/types', '@/views/event/types/index.vue', 'TagsOutlined', 'MENU', 10, 'event:type'),
       (1000012, 1000010, current_timestamp, 1, current_timestamp, 1, 1,
        '事件订阅', '/event/subscriptions', '@/views/event/subscriptions/index.vue', 'ApiOutlined', 'MENU', 20,
        'event:subscription'),
       (1000013, 1000010, current_timestamp, 1, current_timestamp, 1, 1,
        '事件消息', '/event/messages', '@/views/event/messages/index.vue', 'MessageOutlined', 'MENU', 30,
        'event:message'),
       (1000014, 1000010, current_timestamp, 1, current_timestamp, 1, 1,
        '投递管理', '/event/deliveries', '@/views/event/deliveries/index.vue', 'SendOutlined', 'MENU', 40,
        'event:delivery')
on conflict (id) do update
set menu_router = excluded.menu_router, permission_code = excluded.permission_code;

-- 沿用初始化约定，向默认租户（tenant_id=1）开放事件中心及四个页面。
-- 关系 ID 独立于 UPMS test-data.sql 的测试租户授权，防止测试脚本覆盖事件授权。
-- 平台管理员自动拥有全局权限；其他租户和普通角色通过菜单授权功能分配访问范围及读写级别。
insert into sys_tenant_menu_rel (id, create_time, create_by, update_time, update_by, data_status, tenant_id, menu_id)
values (1100201, current_timestamp, 1, current_timestamp, 1, 1, 1, 1000010),
       (1100202, current_timestamp, 1, current_timestamp, 1, 1, 1, 1000011),
       (1100203, current_timestamp, 1, current_timestamp, 1, 1, 1, 1000012),
       (1100204, current_timestamp, 1, current_timestamp, 1, 1, 1, 1000013),
       (1100205, current_timestamp, 1, current_timestamp, 1, 1, 1, 1000014)
on conflict (id) do nothing;

commit;
