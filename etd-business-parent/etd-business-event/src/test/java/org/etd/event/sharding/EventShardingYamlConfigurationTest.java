package org.etd.event.sharding;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.spi.type.typed.TypedSPILoader;
import org.apache.shardingsphere.infra.url.spi.ShardingSphereLocalFileURLLoader;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * ShardingSphere YAML 分表规则装配测试。
 */
class EventShardingYamlConfigurationTest {

    /** 本地分表配置声明的物理分片数量。 */
    private static final int SHARD_COUNT = 2;

    /** ShardingSphere classpath 配置加载器的 SPI 类型。 */
    private static final String CLASSPATH_URL_LOADER_TYPE = "classpath:";

    /** 测试中需要替换的本地 PostgreSQL 环境变量连接模板。 */
    private static final String LOCAL_DATABASE_URL =
            "jdbc:postgresql://$${EVENT_DB_HOST::127.0.0.1}:$${EVENT_DB_PORT::30432}/$${EVENT_DB_NAME::event}";

    /** 测试专用 H2 内存数据库连接地址。 */
    private static final String DATABASE_URL =
            "jdbc:h2:mem:event_sharding_test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";

    /**
     * classpath Driver URL 必须存在对应的配置加载 SPI，防止应用启动时无法读取 YAML。
     */
    @Test
    void shouldLoadClasspathUrlLoader() {
        ShardingSphereLocalFileURLLoader loader = TypedSPILoader.getService(
                ShardingSphereLocalFileURLLoader.class, CLASSPATH_URL_LOADER_TYPE);

        assertNotNull(loader);
    }

    /**
     * 使用真实 YAML 创建逻辑数据源，并校验配置声明的物理表可以完成装配。
     */
    @Test
    void shouldCreateShardingSphereDataSourceWithAllPhysicalTables() throws Exception {
        createPhysicalTables();
        DataSource dataSource = YamlShardingSphereDataSourceFactory.createDataSource(createTestYaml());

        try {
            // 数据源启动阶段会校验逻辑表、单表、物理节点和算法配置能否完整装配。
            try (Connection connection = dataSource.getConnection()) {
                assertNotNull(connection);
            }
        } finally {
            if (dataSource instanceof AutoCloseable closeable) {
                closeable.close();
            }
        }
    }

    private void createPhysicalTables() throws Exception {
        try (Connection connection = DriverManager.getConnection(DATABASE_URL, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("create table evt_event_type (id bigint primary key)");
            statement.execute("create table evt_event_subscription (id bigint primary key)");
            for (int index = 0; index < SHARD_COUNT; index++) {
                String suffix = "_" + index;
                statement.execute("create table evt_event_message" + suffix
                        + " (id bigint primary key, event_id varchar(100) not null)");
                statement.execute("create table evt_event_delivery" + suffix
                        + " (id bigint primary key, event_id varchar(100) not null)");
            }
        }
    }

    private byte[] createTestYaml() throws Exception {
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("sharding-event-local.yaml")) {
            assertNotNull(inputStream);
            String yaml = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("driverClassName: org.postgresql.Driver", "driverClassName: org.h2.Driver")
                    .replace(LOCAL_DATABASE_URL, DATABASE_URL)
                    .replace("$${EVENT_DB_USERNAME::root}", "sa")
                    .replace("$${EVENT_DB_PASSWORD::123456}", "")
                    .replace("$${EVENT_SHARDING_SQL_SHOW::false}", "false")
                    .replace("event_ds.public.evt_event_type", "event_ds.evt_event_type")
                    .replace("event_ds.public.evt_event_subscription", "event_ds.evt_event_subscription");
            return yaml.getBytes(StandardCharsets.UTF_8);
        }
    }

}
