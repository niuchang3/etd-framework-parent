package org.etd.common.permission.constant;

/** 知识库菜单资源码，供资料、目录及跨业务共享接口统一引用。 */
public final class KnowledgePermissionCode {

    /** 资料管理、上传、处理及导航共用的资源码，读写操作由框架权限注解判定。 */
    public static final String DOCUMENT = "ai:knowledge-document";

    /** 知识目录管理资源码，读写操作由框架权限注解判定。 */
    public static final String FOLDER = "ai:knowledge-folder";

    private KnowledgePermissionCode() {
    }
}
