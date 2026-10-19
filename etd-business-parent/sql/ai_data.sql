-- AI 模型管理首次初始化数据，在AI数据库完成ai.sql后显式执行一次。
-- 模型代码、类型及官方指引于2026-10-19核对；账号权限、地域和服务版本仍以厂商控制台为准。
-- 这里只初始化全局模型，不创建租户连接、不保存API Key、不宣称已完成真实模型调用验证。
-- Logo已上传本地MinIO的ai-agent桶，logo_path直接保存可匿名读取的完整图片地址；其他部署环境初始化前按实际对象存储地址调整。
-- 千问Logo来源：https://qwen.ai/（官网引用的阿里CDN图标）
-- 千问原图：https://img.alicdn.com/imgextra/i4/O1CN01OXv3EM1FN8t9W4P79_!!6000000000474-2-tps-80-80.png
-- DeepSeek Logo来源：https://github.com/deepseek-ai.png?size=128（官方组织头像）
-- OpenAI Logo来源：https://github.com/openai.png?size=128（官方组织头像）
-- 同一模型系列共用官方品牌Logo，不编造单个模型专属图标；相关商标归各品牌所有。
-- 模型登记经确认的支持维度集合；知识库从该集合与系统支持范围的交集中选择实际维度，不固定单一输出。
-- OpenAI支持缩减输出，目录登记当前系统可选的768、1024、1536，不穷举服务商的连续缩减范围。
-- 官方模型来源：
-- 百炼聊天：https://help.aliyun.com/zh/model-studio/text-generation-model/
-- 百炼向量：https://help.aliyun.com/zh/model-studio/text-embedding-synchronous-api
-- 百炼重排序：https://help.aliyun.com/zh/model-studio/text-rerank-api
-- 百炼语音合成：https://help.aliyun.com/zh/model-studio/qwen-audio-tts-http-api
-- DeepSeek：https://api-docs.deepseek.com/quick_start/pricing/
-- OpenAI向量：https://developers.openai.com/api/docs/guides/embeddings
-- OpenAI图片：https://developers.openai.com/api/docs/models/gpt-image-2
-- OpenAI语音识别：https://developers.openai.com/api/docs/models/whisper-1
begin;

insert into ai_model (id, create_time, create_by, update_time, update_by, data_status, del_flag, version,
                      provider_code, model_type, model_name, model_code, logo_path, description,
                      api_key_url, setup_guide_url, base_url_help, embedding, sort)
values (2111849786377236481, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'aliyun', 'chat', '千问 Qwen3.8-Max', 'qwen3.8-max', 'http://localhost:30900/ai-agent/model-logos/qwen.png',
        '千问对话模型，支持文本生成和图片理解；主类型为聊天，不是图片生成。',
        'https://help.aliyun.com/zh/model-studio/get-api-key',
        'https://help.aliyun.com/zh/model-studio/text-generation-model/',
        '在百炼控制台选择API Key所属地域和业务空间，复制对应API根地址；OpenAI兼容协议使用以/compatible-mode/v1结尾的地址，DashScope使用该地域的API根地址，不填写完整推理接口路径。', null, 10),
       (2111849786377236482, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'aliyun', 'chat', '千问 Qwen3.7-Plus', 'qwen3.7-plus', 'http://localhost:30900/ai-agent/model-logos/qwen.png',
        '千问对话模型，支持文本生成和图片理解；具体能力及调用参数以官方文档为准。',
        'https://help.aliyun.com/zh/model-studio/get-api-key',
        'https://help.aliyun.com/zh/model-studio/text-generation-model/',
        '在百炼控制台选择API Key所属地域和业务空间，复制对应API根地址；OpenAI兼容协议使用以/compatible-mode/v1结尾的地址，DashScope使用该地域的API根地址，不填写完整推理接口路径。', null, 20),
       (2111849786377236483, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'aliyun', 'chat', '千问 Qwen3.8-Flash', 'qwen3.8-flash', 'http://localhost:30900/ai-agent/model-logos/qwen.png',
        '千问Flash系列文本对话模型，供聊天、文本生成等场景选用。',
        'https://help.aliyun.com/zh/model-studio/get-api-key',
        'https://help.aliyun.com/zh/model-studio/text-generation-model/',
        '在百炼控制台选择API Key所属地域和业务空间，复制对应API根地址；OpenAI兼容协议使用以/compatible-mode/v1结尾的地址，DashScope使用该地域的API根地址，不填写完整推理接口路径。', null, 30),
       (2111849786377236484, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'aliyun', 'embedding', '千问 Qwen3.7 文本向量', 'qwen3.7-text-embedding', 'http://localhost:30900/ai-agent/model-logos/qwen.png',
        '文本向量模型，可用于语义检索和聚类；实际输出维度由知识库选择，须符合服务商支持范围。',
        'https://help.aliyun.com/zh/model-studio/get-api-key',
        'https://help.aliyun.com/zh/model-studio/text-embedding-synchronous-api',
        '在百炼控制台选择API Key所属地域和业务空间，复制对应API根地址；OpenAI兼容协议使用以/compatible-mode/v1结尾的地址，DashScope使用该地域的API根地址，不填写完整推理接口路径。', array[256, 512, 768, 1024, 1536, 2048, 2560], 40),
       (2111849786377236485, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'aliyun', 'embedding', '百炼 Text Embedding V4', 'text-embedding-v4', 'http://localhost:30900/ai-agent/model-logos/qwen.png',
        '文本向量模型，支持多语言语义检索；实际输出维度由知识库选择，须符合服务商支持范围。',
        'https://help.aliyun.com/zh/model-studio/get-api-key',
        'https://help.aliyun.com/zh/model-studio/text-embedding-synchronous-api',
        '在百炼控制台选择API Key所属地域和业务空间，复制对应API根地址；OpenAI兼容协议使用以/compatible-mode/v1结尾的地址，DashScope使用该地域的API根地址，不填写完整推理接口路径。', array[64, 128, 256, 512, 768, 1024, 1536, 2048], 50),
       (2111849786377236486, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'aliyun', 'rerank', '千问 Qwen3.7 文本重排序', 'qwen3.7-text-rerank', 'http://localhost:30900/ai-agent/model-logos/qwen.png',
        '按查询与候选文档的相关性重新排序，适用于检索后的重排序，不是文本向量模型。',
        'https://help.aliyun.com/zh/model-studio/get-api-key',
        'https://help.aliyun.com/zh/model-studio/text-rerank-api',
        '在百炼控制台获取API Key所属地域和业务空间的DashScope API根地址；该模型使用原生重排序接口，请按接入指南配置请求，不使用聊天接口路径。', null, 60),
       (2111849786377236487, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'aliyun', 'text_to_speech', '千问 Qwen Audio 3.0 TTS Plus', 'qwen-audio-3.0-tts-plus', 'http://localhost:30900/ai-agent/model-logos/qwen.png',
        '将文本合成为语音；音色、输出格式及调用方式按官方HTTP API文档配置。',
        'https://help.aliyun.com/zh/model-studio/get-api-key',
        'https://help.aliyun.com/zh/model-studio/qwen-audio-tts-http-api',
        '在百炼控制台获取API Key所属地域和业务空间的DashScope API根地址；该模型使用原生语音合成接口，请按接入指南配置请求，不使用聊天接口路径。', null, 70),
       (2111849786377236488, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'deepseek', 'chat', 'DeepSeek Flash', 'deepseek-flash', 'http://localhost:30900/ai-agent/model-logos/deepseek.png',
        'DeepSeek官方当前Flash调用代码；支持思考及非思考模式，实际版本随官方服务更新。',
        'https://platform.deepseek.com/api_keys',
        'https://api-docs.deepseek.com/',
        '选择OpenAI兼容协议，API根地址按官方文档填写https://api.deepseek.com；使用DeepSeek平台申请的API Key，不使用其他厂商凭证。', null, 80),
       (2111849786377236489, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'deepseek', 'chat', 'DeepSeek V4 Pro', 'deepseek-v4-pro', 'http://localhost:30900/ai-agent/model-logos/deepseek.png',
        'DeepSeek官方V4 Pro对话模型，支持思考及非思考模式；调用标识与官方平台一致。',
        'https://platform.deepseek.com/api_keys',
        'https://api-docs.deepseek.com/',
        '选择OpenAI兼容协议，API根地址按官方文档填写https://api.deepseek.com；使用DeepSeek平台申请的API Key，不使用其他厂商凭证。', null, 90),
       (2111849786377236490, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'openai', 'embedding', 'OpenAI Text Embedding 3 Small', 'text-embedding-3-small', 'http://localhost:30900/ai-agent/model-logos/openai.png',
        '文本向量模型，可用于检索和聚类；实际输出维度由知识库选择，须符合服务商支持范围。',
        'https://platform.openai.com/api-keys',
        'https://developers.openai.com/api/docs/guides/embeddings',
        '使用OpenAI官方API时填写https://api.openai.com/v1作为API根地址；不要填写具体的images/generations或audio/transcriptions路径。模型访问权限以当前组织及项目为准。', array[768, 1024, 1536], 100),
       (2111849786377236491, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'openai', 'image', 'OpenAI GPT Image 2', 'gpt-image-2', 'http://localhost:30900/ai-agent/model-logos/openai.png',
        '图片生成与编辑模型；使用图片接口，不作为聊天模型的图片理解能力分类。',
        'https://platform.openai.com/api-keys',
        'https://developers.openai.com/api/docs/models/gpt-image-2',
        '使用OpenAI官方API时填写https://api.openai.com/v1作为API根地址；不要填写具体的images/generations或audio/transcriptions路径。模型访问权限以当前组织及项目为准。', null, 110),
       (2111849786377236492, current_timestamp, 1, current_timestamp, 1, 1, 0, 0,
        'openai', 'speech_to_text', 'OpenAI Whisper', 'whisper-1', 'http://localhost:30900/ai-agent/model-logos/openai.png',
        '通用语音识别模型，将音频转成文本，可用于多语言转写。',
        'https://platform.openai.com/api-keys',
        'https://developers.openai.com/api/docs/models/whisper-1',
        '使用OpenAI官方API时填写https://api.openai.com/v1作为API根地址；不要填写具体的images/generations或audio/transcriptions路径。模型访问权限以当前组织及项目为准。', null, 120);

commit;
