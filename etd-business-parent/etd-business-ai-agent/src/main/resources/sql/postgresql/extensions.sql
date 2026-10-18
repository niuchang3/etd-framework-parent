/*==============================================================*/
/* AI 数据库检索扩展与中英文分词配置。                          */
/* 在 ai-agent 库显式执行一次，先于本工程 schema.sql。          */
/* 数据库镜像须已安装 vector、zhparser、SCWS 及其词库。         */
/* 不包含系统字典初始化，不由应用启动流程自动执行。            */
/*==============================================================*/
begin;

-- 界面可能已启用扩展；此处只复用已安装扩展，不下载安装文件。
create extension if not exists vector;
create extension if not exists zhparser;

-- SCWS 统一切分中英文，再以英文词干词典规范化；中文词项保持原词，英文词形归一。
create text search configuration public.ai_zh_v1 (parser = zhparser);
alter text search configuration public.ai_zh_v1
    add mapping for n, v, a, i, e, l, j, x, m, t with english_stem;

comment on text search configuration public.ai_zh_v1 is
    'AI 中英文统一分词配置 v1；SCWS 切词及英文词干归一，变更后必须重算全文索引列';

-- 固定 regconfig 使生成列不受连接默认语言影响；配置视为不可变，变更需重算已存词项。
create function public.ai_search_vector(title text, content text)
returns tsvector
language sql
immutable
as $function$
    select pg_catalog.setweight(
               pg_catalog.to_tsvector('public.ai_zh_v1'::regconfig, coalesce(title, '')), 'A')
           || pg_catalog.setweight(
               pg_catalog.to_tsvector('public.ai_zh_v1'::regconfig, coalesce(content, '')), 'B');
$function$;

comment on function public.ai_search_vector(text, text) is
    '生成统一中英文全文词项：标题权重 A、正文权重 B，供分片 STORED 列及 GIN 索引使用';

-- 查询和索引使用相同分词规则；普通输入不作为 tsquery 语法执行。
create function public.ai_search_query(query_text text)
returns tsquery
language sql
immutable
as $function$
    select pg_catalog.plainto_tsquery('public.ai_zh_v1'::regconfig, coalesce(query_text, ''));
$function$;

comment on function public.ai_search_query(text) is
    '按索引相同规则生成中英文全文查询；调用方须对空词项返回空候选并附加租户授权条件';

commit;
