package org.etd.ai.model;

import org.etd.common.permission.constant.ModelPermissionCode;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.regex.Pattern;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 执行共享菜单DDL和首次初始化数据，验证首页聚合入口和平台模型目录的路由及权限契约。 */
class ModelSquareMenuInitializationTest {
    /** 首页统一提供模型浏览和连接管理，初始化不再提供独立连接器菜单。 */
    @Test
    void shouldInitializeUnifiedSquareMenu() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:home-menu" + System.nanoTime() + ";MODE=PostgreSQL");
        try (Connection connection = source.getConnection()) {
            initializeMenus(connection);
            verifyMenu(connection, "/dashboard", "@/views/dashboard/index.vue", ModelPermissionCode.SQUARE);
            verifyStandaloneConnectionMenuRemoved(connection);
            verifyMenu(connection, "/ai/models", "@/views/ai/model/index.vue", ModelPermissionCode.MODEL);
            // 知识库组件路径按前端实际返回值保存，不擅自追加@别名。
            verifyMenu(connection, "/ai/knowledge-bases", "@/views/ai/knowledge-base/index.vue", "ai:knowledge-base");
        }
    }

    /** 旧连接器菜单和资源码均不再初始化，避免用户被要求同时获取两个菜单授权。 */
    private void verifyStandaloneConnectionMenuRemoved(Connection connection) throws Exception {
        String query = "select count(*) from sys_menus where menu_path = ? or permission_code = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, "/ai/model-connections");
            statement.setString(2, "ai:model-connection");
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(0, rows.getInt(1));
            }
        }
    }

    /** 使用生产菜单表结构及全部菜单初始化语句，避免测试复制一套近似菜单数据。 */
    private void initializeMenus(Connection connection) throws Exception {
        ClassPathResource schema = new ClassPathResource("sql/upms.sql");
        String ddl = schema.getContentAsString(StandardCharsets.UTF_8);
        int start = ddl.indexOf("create table if not exists sys_menus\n");
        int end = ddl.indexOf(';', start);
        String menuDdl = ddl.substring(start, end);
        ClassPathResource resource = new ClassPathResource("sql/init.sql");
        String seed = resource.getContentAsString(StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile("insert into sys_menus\\s*\\(.*?;", Pattern.DOTALL);
        Matcher statements = pattern.matcher(seed);
        try (Statement statement = connection.createStatement()) {
            statement.execute(menuDdl);
            while (statements.find()) {
                // 本用例验证首次初始化；仅移除H2不支持的PostgreSQL冲突更新子句。
                String sql = statements.group().replaceFirst("(?s)\\s+on conflict.*;$", ";");
                statement.execute(sql);
            }
        }
    }

    /** 路由只对应一个页面，权限与实际API声明保持一致，不再使用404占位组件。 */
    private void verifyMenu(Connection connection, String path, String component, String permission) throws Exception {
        String query = "select menu_router, permission_code, data_status from sys_menus where menu_path = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, path);
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(component, rows.getString("menu_router"));
                assertEquals(permission, rows.getString("permission_code"));
                assertEquals(1, rows.getInt("data_status"));
                assertFalse(rows.next());
            }
        }
    }
}
