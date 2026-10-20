-- 公共字典、菜单和系统参数初始化，统一在UPMS数据库执行。
-- 先执行upms.sql；本文件不包含租户账号、组织或租户菜单授权。
begin;

-- 首页模型广场统一承接模型浏览与连接接入管理，按read/write授权，不另设连接器菜单。
-- sys_menus 初始化。
insert into sys_menus (id, parent_id, create_time, create_by, update_time, update_by, data_status, menu_name,
                       menu_path, menu_router, menu_icon, menu_type, sort, permission_code)
values (1000001, null, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '首页', '/dashboard', '@/views/dashboard/index.vue', 'AppstoreOutlined', 'MENU', 10, 'ai:model-square'),
       (1000002, null, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '租户管理', '/system/tenants', '@/views/tenant/index.vue', 'TeamOutlined', 'MENU', 20, 'system:tenant'),
       (1000003, null, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '用户中心', '/system/users', '@/views/system/users/index.vue', 'UserOutlined', 'MENU', 30, 'system:user'),
       (1000004, null, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '系统管理', '/system', null, 'SettingOutlined', 'DIRECTORY', 40, null),
       (1000005, 1000004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '系统字典', '/system/dictionaries', '@/views/system/dictionaries/index.vue', 'DatabaseOutlined', 'MENU', 10, 'system:dict'),
       (1000006, 1000004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '角色管理', '/system/roles', '@/views/system/roles/index.vue', 'SafetyCertificateOutlined', 'MENU', 20, 'system:role'),
       (1000007, 1000004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '菜单管理', '/system/menus', '@/views/system/menus/index.vue', 'MenuOutlined', 'MENU', 30, 'system:menu'),
       (1000008, 1000004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '组织架构', '/system/organizations', '@/views/system/organizations/index.vue', 'ApartmentOutlined', 'MENU', 40, 'system:organization'),
       (1000009, 1000004, '2026-08-30 04:57:41.475264+08:00', 1, '2026-08-30 04:57:41.475264+08:00', 1, 1,
        '系统配置', '/system/config', '@/views/system/config/index.vue', 'SettingOutlined', 'MENU', 50, 'system:config')
on conflict (id) do update
set menu_router = excluded.menu_router, permission_code = excluded.permission_code;

-- sys_dict_type 初始化。
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

-- sys_dict_data 初始化。
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

-- sys_dict_type 初始化。
insert into sys_dict_type (id, create_time, create_by, update_time, update_by, data_status, del_flag,
                           type_code, type_name, built_in, enabled, remark)
values (2200101, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_model_type', 'AI 模型类型', true, true, '模型主能力分类；实际可选项还需由后端适配器能力筛选'),
       (2200102, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_model_protocol', 'AI 模型接入协议', true, true, '模型服务适配器类别，不是模型厂商品牌列表'),
       (2200103, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_model_auth_type', 'AI 模型认证方式', true, true, '根据接入协议校验允许的认证方式，不包含 SaaS 内部加密主密钥'),
       (2200104, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_model_provider', 'AI 模型服务商', true, true, '模型管理所属服务商，用于分类筛选，与调用协议分开'),
       (2200105, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_embedding_dimension', 'AI 向量维度', true, true, '系统向量存储支持的维度；知识库可选项须与所选模型的支持维度取交集');

-- sys_dict_data 初始化。
insert into sys_dict_data (id, create_time, create_by, update_time, update_by, data_status, del_flag,
                           dict_type_id, dict_code, dict_label, dict_value, sort, built_in, enabled, remark)
-- AI 字典值与对应业务表 CHECK 中的代码保持一致，不保存租户模型实例或凭证。
values (2300201, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200101, 'CHAT', '聊天模型', 'chat', 10, true, true, '对话与文本生成；是否支持工具调用或图片输入由模型能力另行描述'),
       (2300202, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200101, 'EMBEDDING', '向量模型', 'embedding', 20, true, true, '将文本转换为向量；固定维度由平台目录维护，接入仍需验证'),
       (2300203, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200101, 'RERANK', '重排序模型', 'rerank', 30, true, true, '对召回结果重新排序；页面仅在后端具备对应适配能力时开放选择'),
       (2300204, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200101, 'IMAGE', '图片生成模型', 'image', 40, true, true, '用于生成图片，不表示聊天模型的图片理解能力'),
       (2300205, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200101, 'SPEECH_TO_TEXT', '语音识别模型', 'speech_to_text', 50, true, true, '将语音转换为文本'),
       (2300206, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200101, 'TEXT_TO_SPEECH', '语音合成模型', 'text_to_speech', 60, true, true, '将文本转换为语音'),
       (2300207, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200102, 'DASHSCOPE', 'DashScope 协议', 'dashscope', 10, true, true, '百炼模型服务的 DashScope 接入协议'),
       (2300208, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200102, 'OLLAMA', 'Ollama 协议', 'ollama', 20, true, true, '通过 SaaS 后端可访问的 Ollama 服务接入'),
       (2300209, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200102, 'OPENAI_COMPATIBLE', 'OpenAI 兼容协议', 'openai_compatible', 30, true, true, '接入实现相应协议的模型服务，不限定厂商；仍需核验具体能力'),
       (2300210, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200103, 'NONE', '无需认证', 'none', 10, true, true, '只适用于允许无认证访问的模型服务，不填写认证凭证'),
       (2300211, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200103, 'API_KEY', 'API Key', 'api_key', 20, true, true, '单个 API Key 凭证，后端加密保存；不额外要求 Secret Key'),
       (2300212, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200104, 'ALIYUN', '阿里云百炼', 'aliyun', 10, true, true, '服务商品牌，不限定租户选择的协议'),
       (2300213, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200104, 'OPENAI', 'OpenAI', 'openai', 20, true, true, '服务商品牌，不表示OpenAI兼容协议'),
       (2300214, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200104, 'DEEPSEEK', 'DeepSeek', 'deepseek', 30, true, true, '服务商品牌，具体模型由平台目录维护'),
       (2300215, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200105, 'DIMENSION_768', '768 维', '768', 10, true, true, '对应系统 768 维向量存储'),
       (2300216, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200105, 'DIMENSION_1024', '1024 维', '1024', 20, true, true, '对应系统 1024 维向量存储'),
       (2300217, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200105, 'DIMENSION_1536', '1536 维', '1536', 30, true, true, '对应系统 1536 维向量存储');

-- sys_menus 初始化。
insert into sys_menus (id, parent_id, create_time, create_by, update_time, update_by, data_status, del_flag,
                       menu_name, menu_path, menu_router, permission_code, menu_icon, menu_type, sort)
values (1000101, null, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'AI管理', '/ai', null, null, 'RobotOutlined', 'DIRECTORY', 30),
       (1000106, 1000101, current_timestamp, 1, current_timestamp, 1, 1, 0,
        '模型管理', '/ai/models', '@/views/ai/model/index.vue',
        'ai:model', 'AppstoreOutlined', 'MENU', 10),
       (1000105, 1000101, current_timestamp, 1, current_timestamp, 1, 1, 0,
        '知识库分类', '/ai/knowledge-folders', '@/views/ai/knowledge-folder/index.vue',
        'ai:knowledge-folder', 'ApartmentOutlined', 'MENU', 30);

-- sys_menus 初始化。
insert into sys_menus (id, parent_id, create_time, create_by, update_time, update_by, data_status, menu_name,
                       menu_path, menu_router, menu_icon, menu_type, sort, permission_code)
values (1000010, null, current_timestamp, 1, current_timestamp, 1, 1,
        '事件中心', '/event', null, 'NotificationOutlined', 'DIRECTORY', 30, null),
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

-- sys_config 初始化。
insert into sys_config (id, create_time, create_by, update_time, update_by, data_status, parameter_key,
                        parameter_name, parameter_value, value_type, built_in, enabled, remark)
values (4000001, current_timestamp, 1, current_timestamp, 1, 1,
        'system.branding', '系统视觉与版权信息',
        '{"name":"ETD 后台管理系统","subtitle":"Operations Suite","logo":"","favicon":"/favicon.ico","copyright":"Copyright © 2026 ETD. All Rights Reserved.","eyebrow":"Enterprise Operations Console","title":"统一运营管理平台","description":"集中管理任务、资源和平台运行状态。","region":"CN-EAST-1","version":"v2.8.4","securityText":"Secure access · TLS 1.3","formEyebrow":"Account Access","formTitle":"登录控制台","formDescription":"请输入你的账号和密码以继续。","auditNote":"仅限已授权的平台账号访问。登录行为将被安全审计。","serviceStatus":"Operational","watermark":{"enabled":false,"opacity":0.15,"fontSize":14}}',
        'json', true, true, '全局品牌名、Logo链接、水印配置'),
       (4000002, current_timestamp, 1, current_timestamp, 1, 1,
        'security.policy', '登录验证码与密码策略',
        '{"captcha":{"triggerOnFailCount":5},"password":{"minLength":8,"regexp":"^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&_#.~-]{8,72}$","defaultPassword":"Etd@123456"}}',
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

commit;
