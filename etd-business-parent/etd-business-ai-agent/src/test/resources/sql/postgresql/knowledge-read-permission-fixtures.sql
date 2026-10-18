-- 仅在加载当前schema.sql的空测试库中使用，覆盖租户、公开范围、逻辑删除及后台清理边界。
insert into ai_knowledge_base(id,tenant_id,knowledge_name,chunk_max_tokens,chunk_overlap_tokens,
  keyword_top_k,vector_top_k,retrieval_top_k)
  select id,tenant_id,name,512,50,10,10,5 from (values
    (1,1,'本库'),(2,1,'另一知识库'),(3,1,'停用知识库'),(4,2,'另一租户')) v(id,tenant_id,name);
update ai_knowledge_base set data_status=0 where id=3;
insert into ai_knowledge_folder(id,tenant_id,knowledge_base_id,parent_id,folder_name,org_id,create_by,is_public) values
  (1,1,1,null,'私有祖先',20,200,false),(2,1,1,1,'公开文件入口',20,200,false),
  (3,1,1,1,'隐藏同级',20,200,false),(4,1,1,null,'公开目录',20,200,true),
  (5,1,1,null,'本人目录',20,100,false),(6,1,1,null,'部门目录',10,200,false),
  (7,1,1,null,'隐藏根目录',20,200,false),(8,1,2,null,'其他库目录',20,200,true),
  (9,2,4,null,'其他租户目录',20,200,true);
insert into ai_knowledge_document(id,tenant_id,knowledge_base_id,folder_id,document_name,original_filename,
  storage_bucket,storage_object_key,content_type,file_size,file_sha256,org_id,create_by,is_public) values
  (101,1,1,2,'公开文件','a.txt','test','101','text/plain',1,repeat('a',64),20,200,true),
  (102,1,1,3,'隐藏文件','a.txt','test','102','text/plain',1,repeat('a',64),20,200,false),
  (103,1,1,5,'本人文件','a.txt','test','103','text/plain',1,repeat('a',64),20,100,false),
  (104,1,1,6,'部门文件','a.txt','test','104','text/plain',1,repeat('a',64),10,200,false),
  (105,2,4,9,'其他租户','a.txt','test','105','text/plain',1,repeat('a',64),20,200,true),
  (106,1,2,8,'其他库','a.txt','test','106','text/plain',1,repeat('a',64),20,200,true),
  (107,1,1,null,'已删除','a.txt','test','107','text/plain',1,repeat('a',64),20,200,true),
  (108,1,1,null,'已停用','a.txt','test','108','text/plain',1,repeat('a',64),20,200,true),
  (109,1,3,null,'知识库已停用','a.txt','test','109','text/plain',1,repeat('a',64),20,200,true),
  (110,1,1,null,'未分类','a.txt','test','110','text/plain',1,repeat('a',64),20,200,true);
update ai_knowledge_document set del_flag=1 where id=107;
update ai_knowledge_document set data_status=0 where id=108;
update ai_knowledge_document set folder_id=7 where id=107;
insert into ai_model_connection(id,tenant_id,connection_name,provider,base_url,auth_type)
  values (201,1,'模型连接','ollama','http://model.invalid','none');
insert into ai_model_config(id,tenant_id,connection_id,model_name,model_type,model_code,embedding_dimensions)
  values (301,1,201,'向量模型','embedding','test-embedding',1024);
insert into ai_knowledge_base(id,tenant_id,knowledge_name,embedding_model_config_id,embedding_dimensions,
  chunk_max_tokens,chunk_overlap_tokens,keyword_top_k,vector_top_k,retrieval_top_k,del_flag) values
  (5,1,'已删除知识库',301,1024,512,50,10,10,5,1),(6,1,'已绑定知识库',301,1024,512,50,10,10,5,0);
insert into ai_knowledge_folder(id,tenant_id,knowledge_base_id,folder_name,org_id,create_by,is_public,del_flag)
  values (10,1,1,'已删除目录',20,200,true,1);
insert into ai_knowledge_document(id,tenant_id,knowledge_base_id,document_name,original_filename,
  storage_bucket,storage_object_key,content_type,file_size,file_sha256,org_id,create_by,is_public)
  values (112,1,5,'已删除库的文件','a.txt','test','112','text/plain',1,repeat('a',64),20,200,true);
insert into ai_storage_cleanup(id,tenant_id,bucket,object_key,abort_multipart,del_flag) values
  (501,1,'test','101',false,0),(502,1,'test','107',false,0),
  (503,2,'test','unused',false,0),(504,1,'test','old',false,1);

-- 停用目录仍可在管理页面查询，筛选时保留其必要祖先。
update ai_knowledge_folder set data_status = 0 where id = 2;

-- 连接与模型管理页也必须同时支持全部、启用及禁用记录。
insert into ai_model_connection(id, tenant_id, connection_name, provider, base_url, auth_type, data_status)
values (202, 1, '停用连接', 'ollama', 'http://model.invalid', 'none', 0);
insert into ai_model_config(id, tenant_id, connection_id, model_name, model_type, model_code, embedding_dimensions, data_status)
values (302, 1, 202, '停用模型', 'embedding', 'disabled-embedding', 1024, 0);
