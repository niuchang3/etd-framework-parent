package org.etd.framework.starter.ai.document;

import org.etd.framework.starter.ai.document.autoconfigure.DocumentParserAutoConfiguration;
import org.etd.framework.starter.ai.document.docling.DoclingDocumentParser;
import org.etd.framework.starter.ai.document.parser.DocumentParserRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 自动配置的显式启用、配置校验与替换行为，不访问远端。 */
class DocumentParserAutoConfigurationTest {
    private final AutoConfigurations configurations = AutoConfigurations.of(DocumentParserAutoConfiguration.class);
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withConfiguration(configurations);

    @Test
    void disabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(DoclingDocumentParser.class);
            assertThat(context).hasSingleBean(DocumentParserRegistry.class);
        });
    }

    @Test
    void enabledCreatesSingletonWithoutConnecting() {
        runner.withPropertyValues("etd.ai.document.docling.enabled=true",
                "etd.ai.document.docling.base-url=http://127.0.0.1:1")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(DoclingDocumentParser.class);
                    var parser = context.getBean(DoclingDocumentParser.class);
                    var registry = context.getBean(DocumentParserRegistry.class);
                    assertThat(registry.require(DoclingDocumentParser.ENGINE)).isSameAs(parser);
                });
    }

    @Test
    void enabledRequiresValidConfiguration() {
        runner.withPropertyValues("etd.ai.document.docling.enabled=true")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("etd.ai.document.docling.enabled=true",
                "etd.ai.document.docling.base-url=http://127.0.0.1:1",
                "etd.ai.document.docling.read-timeout=0s")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void explicitParserReplacesDefaultWithoutRequiringConnectionProperties() {
        var parser = mock(DoclingDocumentParser.class);
        when(parser.engine()).thenReturn(DoclingDocumentParser.ENGINE);
        runner.withBean(DoclingDocumentParser.class, () -> parser)
                .withPropertyValues("etd.ai.document.docling.enabled=true")
                .run(context -> assertThat(context.getBean(DoclingDocumentParser.class)).isSameAs(parser));
    }
}
