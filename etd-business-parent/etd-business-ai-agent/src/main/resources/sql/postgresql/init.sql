/*==============================================================*/
/* AI 业务字典及系统参数初始化，由 AI 工程维护。               */
/* 连接 sys_dict_type、sys_dict_data、sys_config 所在库执行。   */
/* 执行前先完成 UPMS schema.sql；不在 AI 库创建重复系统表。      */
/* 本脚本不接入应用启动自动执行，记录均为全局模块级配置。      */
/*==============================================================*/
begin;

insert into sys_dict_type (id, create_time, create_by, update_time, update_by, data_status, del_flag,
                           type_code, type_name, built_in, enabled, remark)
values (2200101, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_model_type', 'AI 模型类型', true, true, '模型主能力分类；实际可选项还需由后端适配器能力筛选'),
       (2200102, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_model_provider', 'AI 模型接入协议', true, true, '模型服务适配器类别，不是模型厂商品牌列表'),
       (2200103, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_model_auth_type', 'AI 模型认证方式', true, true, '根据接入协议校验允许的认证方式，不包含 SaaS 内部加密主密钥'),
       (2200104, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_knowledge_document_status', '知识库文档处理状态', true, true, '用于列表筛选和状态展示，状态由后端工作流维护'),
       (2200105, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_knowledge_chunk_embedding_status', '知识库分片向量化状态', true, true, '用于分片状态展示和筛选，不允许用户直接修改处理状态'),
       (2200106, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai_knowledge_retrieval_mode', '知识库检索模式', true, true, '选择全文、向量或混合检索；后端校验模型绑定及索引发布状态')
    on conflict (id) do nothing;

insert into sys_dict_data (id, create_time, create_by, update_time, update_by, data_status, del_flag,
                           dict_type_id, dict_code, dict_label, dict_value, sort, built_in, enabled, remark)
-- AI 字典值与模型表 CHECK 及知识库设计中的代码保持一致，不保存租户模型实例或凭证。
values (2300225, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200104, 'UPLOADED', '已上传', 'uploaded', 0, true, true, '仅原件上传完成，尚未解析、分片或向量化'),
       (2300201, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200101, 'CHAT', '聊天模型', 'chat', 10, true, true, '对话与文本生成；是否支持工具调用或图片输入由模型能力另行描述'),
       (2300202, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200101, 'EMBEDDING', '向量模型', 'embedding', 20, true, true, '将文本转换为向量；知识库绑定时实测维度并校验支持范围'),
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
        2200104, 'PENDING', '待处理', 'pending', 10, true, true, '文件已登记，等待开始解析'),
       (2300213, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200104, 'PARSING', '解析中', 'parsing', 20, true, true, '正在提取文件内容并生成分片'),
       (2300214, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200104, 'PREVIEW', '待确认分片', 'preview', 30, true, true, '分片已保存，可预览和调整，尚未发布检索'),
       (2300215, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200104, 'EMBEDDING', '向量化中', 'embedding', 40, true, true, '正在调用 Embedding 模型并写入向量'),
       (2300216, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200104, 'READY', '已完成', 'ready', 50, true, true, '满足当前检索模式的发布要求；纯全文模式不要求已生成向量'),
       (2300217, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200104, 'FAILED', '处理失败', 'failed', 60, true, true, '本轮处理失败，具体原因读取脱敏错误摘要'),
       (2300218, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200105, 'PENDING', '待向量化', 'pending', 10, true, true, '分片尚未成功生成并写入当前正文对应的向量'),
       (2300219, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200105, 'READY', '已完成', 'ready', 20, true, true, '当前分片正文对应的向量已成功写入'),
       (2300220, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200105, 'FAILED', '向量化失败', 'failed', 30, true, true, '当前分片向量化失败，可由后端重试流程重新处理'),
       (2300221, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200105, 'NOT_REQUIRED', '无需向量化', 'not_required', 0, true, true, '纯全文模式未启用向量，不调用 Embedding 模型'),
       (2300222, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200106, 'KEYWORD', '全文检索', 'keyword', 10, true, true, '使用中英文分词及全文索引召回，不调用 Embedding 模型'),
       (2300223, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200106, 'VECTOR', '向量检索', 'vector', 20, true, true, '使用知识库绑定的 Embedding 模型进行语义相似度检索'),
       (2300224, current_timestamp, 1, current_timestamp, 1, 1, 0,
        2200106, 'HYBRID', '混合检索', 'hybrid', 30, true, true, '分别执行全文和向量召回，按分片去重后使用 RRF 融合排序')
    on conflict (id) do nothing;

/*==============================================================*/
/* AI 文件上传参数：仅控制文件接收，不表示解析器已支持该格式。 */
/* 不复用 system.resource.limit，各模块按业务前缀维护自身策略。 */
/*==============================================================*/
insert into sys_config (id, create_time, create_by, update_time, update_by, data_status, del_flag,
                        parameter_key, parameter_name, parameter_value, value_type, built_in, enabled, remark)
values (4000101, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai-agent-file-type', 'AI 知识库允许上传的文件类型',
        '{"allowedExtensions":[".txt",".md",".pdf",".docx",".xlsx",".csv"]}',
        'json', true, true,
        'AI 模块专属文件扩展名白名单：小写且带点；仅由前端组件读取校验；允许上传不代表已支持解析'),
       (4000102, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'ai-agent-file-upload', 'AI 知识库分片上传策略',
        '{"maxFileSizeBytes":104857600,"partSizeBytes":8388608,"concurrency":3,"maxRetries":3,"urlExpirySeconds":900,"sessionExpireSeconds":86400}',
        'json', true, true,
        '单文件上限100MiB、分片8MiB、组件并发3、失败后最多重试3次；签名900秒、任务86400秒；客户端校验文件类型及大小，传输参数随初始化请求提交并保存快照')
    on conflict (id) do nothing;

commit;



/*==============================================================*/
/* AI 管理菜单初始化，由 AI 工程维护。                          */
/* 在 UPMS 系统表所在数据库执行，先完成 UPMS schema.sql、data.sql。 */
/* 本脚本手动执行，不接入 AI 应用启动时的数据库初始化。          */
/* 菜单 ID：1000101～1000106；默认租户菜单关系 ID：1100101～1100106。 */
/* data_status=1 表示启用，del_flag=0 表示未删除；操作人沿用初始管理员 1。 */
/*==============================================================*/
begin;

-- 目录只组织导航，不绑定组件和资源权限；页面路径统一使用以 / 开头的完整路径。
-- permission_code保存资源码，框架按read/write授权校验接口；目录节点为空。
-- 前端按钮同时检查页面write权限和目录、文件的writable数据权限。
insert into sys_menus (id, parent_id, create_time, create_by, update_time, update_by, data_status, del_flag,
                       menu_name, menu_path, menu_router, permission_code, menu_icon, menu_type, sort)
values (1000101, null, current_timestamp, 1, current_timestamp, 1, 1, 0,
        'AI管理', '/ai', null, null, 'RobotOutlined', 'DIRECTORY', 50),
       (1000102, 1000101, current_timestamp, 1, current_timestamp, 1, 1, 0,
        '模型连接', '/ai/model-connections', '@/views/ai/model-connection/index.vue',
        'ai:model-connection', 'LinkOutlined', 'MENU', 10),
       (1000103, 1000101, current_timestamp, 1, current_timestamp, 1, 1, 0,
        '模型配置', '/ai/model-configs', '@/views/ai/model-config/index.vue',
        'ai:model-config', 'ControlOutlined', 'MENU', 20),
       (1000104, 1000101, current_timestamp, 1, current_timestamp, 1, 1, 0,
        '知识库', '/ai/knowledge-bases', '@/views/ai/knowledge-base/index.vue',
        'ai:knowledge-base', 'BookOutlined', 'MENU', 30),
       (1000105, 1000101, current_timestamp, 1, current_timestamp, 1, 1, 0,
        '知识库目录', '/ai/knowledge-folders', '@/views/ai/knowledge-folder/index.vue',
        'ai:knowledge-folder', 'ApartmentOutlined', 'MENU', 40),
       (1000106, 1000101, current_timestamp, 1, current_timestamp, 1, 1, 0,
        '知识库文件', '/ai/knowledge-documents', '@/views/ai/knowledge-document/index.vue',
        'ai:knowledge-document', 'FileTextOutlined', 'MENU', 50)
on conflict (id) do nothing;

-- 沿用 UPMS 初始化约定，向默认租户（tenant_id=1）开放 AI 目录及全部页面。
-- 菜单资源授权控制接口read/write能力，记录访问范围由框架租户及数据权限控制。
insert into sys_tenant_menu_rel (id, create_time, create_by, update_time, update_by, data_status, del_flag,
                                 tenant_id, menu_id)
values (1100101, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000101),
       (1100102, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000102),
       (1100103, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000103),
       (1100104, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000104),
       (1100105, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000105),
       (1100106, current_timestamp, 1, current_timestamp, 1, 1, 0, 1, 1000106)
on conflict (id) do nothing;

commit;
