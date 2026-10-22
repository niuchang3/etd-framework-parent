package org.etd.framework.starter.ai.document.autoconfigure;

import java.net.http.HttpClient;
import java.util.List;
import org.etd.framework.starter.ai.document.docling.DoclingDocumentParser;
import org.etd.framework.starter.ai.document.docling.DoclingProperties;
import org.etd.framework.starter.ai.document.docling.DoclingTaskClient;
import org.etd.framework.starter.ai.document.docling.converter.DoclingResultConverter;
import org.etd.framework.starter.ai.document.parser.DocumentParser;
import org.etd.framework.starter.ai.document.parser.DocumentParserRegistry;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** 文档解析按配置启用；没有解析服务配置时仍可正常使用 AI starter。 */
@AutoConfiguration
@EnableConfigurationProperties(DoclingProperties.class)
public class DocumentParserAutoConfiguration {
    /** 只有配置值为 true 时才装配远程解析器。 */
    private static final String ENABLED_VALUE = "true";

    /**
     * @param parsers 应用已注册的解析引擎
     * @return 可扩展的引擎注册表，允许没有任何引擎
     */
    @Bean
    @ConditionalOnMissingBean
    DocumentParserRegistry documentParserRegistry(List<DocumentParser> parsers) {
        return new DocumentParserRegistry(parsers);
    }

    /**
     * @param properties 只有启用时才校验的连接配置
     * @return 复用的 Docling 解析器，不在启动时发送请求
     */
    @Bean
    @ConditionalOnMissingBean(DoclingDocumentParser.class)
    @ConditionalOnProperty(prefix = DoclingProperties.PREFIX, name = DoclingProperties.ENABLED_PROPERTY,
            havingValue = ENABLED_VALUE)
    DoclingDocumentParser doclingDocumentParser(DoclingProperties properties) {
        properties.validate();
        var httpBuilder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER);
        var client = DoclingTaskClient.builder().baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey()).connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout()).httpClientBuilder(httpBuilder)
                .logRequests(false).logResponses(false).build();
        DoclingResultConverter converter = Mappers.getMapper(DoclingResultConverter.class);
        return new DoclingDocumentParser(client, converter, properties.getMaxFileBytes());
    }
}
