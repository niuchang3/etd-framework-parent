# 本轮增量：保留框架接口权限，移除后端常量类

后端仅删除AiPermission常量类，六个Controller继续使用框架 `@Permission`，注解直接填写原资源码。read/write权限、接口路径、请求响应和writable含义均保持不变。

前端若尚未执行上一轮“移除功能权限”提示词，无需修改。若已执行，则只恢复以下内容：

1. 恢复五类AI页面及分片上传原有read/write权限判断，包括路由、按钮和组件禁用条件。
2. 目录、文件的操作按钮同时要求对应write权限和后端writable=true。公开资料仍要求功能read权限。
3. 五个AI页面菜单的permissionCode依次为 `ai:model-connection`、`ai:model-config`、`ai:knowledge-base`、`ai:knowledge-folder`、`ai:knowledge-document`；AI根目录permissionCode为null。权限码不带read/write后缀。
4. 上传使用 `ai:knowledge-document:write`；查询使用对应资源的read权限。
5. 登录校验、Bearer Token、401/403处理、字典及系统参数保持原样。

已将现有菜单permissionCode清空的环境，需要通过菜单维护恢复上述资源码；初始化脚本的on conflict do nothing不会覆盖已有记录。本次未修改运行中的数据库。
