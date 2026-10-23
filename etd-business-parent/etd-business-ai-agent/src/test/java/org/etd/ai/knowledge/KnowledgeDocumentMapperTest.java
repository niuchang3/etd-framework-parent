package org.etd.ai.knowledge;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.etd.ai.knowledge.mapper.KnowledgeDocumentMapper;
import org.etd.ai.knowledge.model.UploadPart;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 加载生产Mapper XML，验证显式映射保留驼峰字段和上传回执的JSON转换。 */
class KnowledgeDocumentMapperTest {
    /** 过期扫描只返回到期上传，映射后的回执元素仍为UploadPart。 */
    @Test
    void shouldMapExpiredUploadsWithExplicitResultMap() throws Exception {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:document-mapper-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        populateUploadRows(dataSource);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        var configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);
        var resource = new ClassPathResource("mapper/knowledge/KnowledgeDocumentMapper.xml");
        factory.setMapperLocations(resource);
        var sessionFactory = factory.getObject();
        try (var session = sessionFactory.openSession()) {
            var mapper = session.getMapper(KnowledgeDocumentMapper.class);
            var result = mapper.selectExpiredUploadList();
            assertEquals(1, result.size());
            var document = result.getFirst();
            assertEquals(1L, document.getId());
            assertEquals("资料.PDF", document.getOriginalFilename());
            assertEquals("pdf", document.getFileType());
            assertEquals(Instant.parse("2020-01-01T00:00:00Z"), document.getUploadExpiresAt());
            var part = new UploadPart(1, "test-etag", 10L);
            var expectedParts = List.of(part);
            assertEquals(expectedParts, document.getUploadParts());
        }
    }

    /** 仅提供扫描SQL使用的字段；JSON以文本模拟JDBC读取，不模拟PostgreSQL存储行为。 */
    private void populateUploadRows(JdbcDataSource dataSource) throws Exception {
        String ddl = """
                create table ai_knowledge_document (
                    id bigint primary key, original_filename varchar(255), file_type varchar(255),
                    del_flag integer, upload_status varchar(20),
                    upload_expires_at timestamp with time zone, upload_parts varchar(1000)
                )
                """;
        String insert = """
                insert into ai_knowledge_document values
                    (1, '资料.PDF', 'pdf', 0, 'uploading', timestamp with time zone '2020-01-01 00:00:00+00',
                     '[{"partNumber":1,"etag":"test-etag","size":10}]'),
                    (2, '已完成.pdf', 'pdf', 0, 'completed', timestamp with time zone '2020-01-01 00:00:00+00', '[]'),
                    (3, '未到期.pdf', 'pdf', 0, 'uploading', current_timestamp + interval '1' day, '[]')
                """;
        try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(ddl);
            statement.execute(insert);
        }
    }
}
