package org.etd.ai.knowledge;

import com.alibaba.druid.wall.spi.PGWallProvider;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.Configuration;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** 使用生产Mapper生成SQL，验证Druid PostgreSQL防火墙能解析目标表行锁。 */
class KnowledgeLockSqlTest {
    /** 覆盖知识库和文档的加锁、非加锁查询。 */
    @Test
    void shouldPassWallForScopedReadAndLockQueries() throws Exception {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:knowledge-lock-sql");
        var bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(source);
        var configuration = new MybatisConfiguration();
        bean.setConfiguration(configuration);
        var base = new ClassPathResource("mapper/knowledge/KnowledgeBaseMapper.xml");
        var document = new ClassPathResource("mapper/knowledge/KnowledgeDocumentMapper.xml");
        bean.setMapperLocations(base, document);
        var factory = bean.getObject();
        for (boolean lock : List.of(false, true)) {
            verifyWallSql(factory.getConfiguration(), "KnowledgeBaseMapper", lock);
            verifyWallSql(factory.getConfiguration(), "KnowledgeDocumentMapper", lock);
        }
    }

    /** 校验完整权限及关联查询，避免仅对简化SQL验证而遗漏实际防火墙拒绝。 */
    private void verifyWallSql(Configuration configuration, String mapper,
                               boolean lock) {
        var parameters = Map.of("id", 1L, "lock", lock);
        var statement = configuration.getMappedStatement("org.etd.ai.knowledge.mapper." + mapper + ".selectAccessible");
        var sql = statement.getBoundSql(parameters).getSql();
        var provider = new PGWallProvider();
        assertThat(provider.check(sql).getViolations()).as("%s lock=%s", mapper, lock).isEmpty();
    }
}
