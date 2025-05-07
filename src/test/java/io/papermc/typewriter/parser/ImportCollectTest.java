package io.papermc.typewriter.parser;

import imports.FancyCommentImportType;
import imports.FancyInlinedImportType;
import imports.FancyNewlineImportType;
import imports.FancySpaceImportType;
import imports.MixedCommentImportType;
import imports.StandardImportType;
import imports.UnicodeImportType;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.context.ImportCategory;
import io.papermc.typewriter.context.ImportSet;
import io.papermc.typewriter.context.ImportNameCollector;
import io.papermc.typewriter.util.ClassResolver;
import io.papermc.typewriter.yaml.ImportMapping;
import io.papermc.typewriter.yaml.YamlMappingConverter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImportCollectTest extends ParserTest {

    private static Arguments fileToArgs(Class<?> sampleClass) {
        return Arguments.of(
            CONTAINER.resolve(sampleClass.getCanonicalName().replace('.', '/') + ".java"),
            sampleClass,
            "expected/imports/%s.yaml".formatted(sampleClass.getSimpleName())
        );
    }

    private static Stream<Arguments> fileProvider() {
        return Stream.of(
            StandardImportType.class,
            UnicodeImportType.class,
            FancySpaceImportType.class,
            FancyCommentImportType.class,
            FancyNewlineImportType.class,
            FancyInlinedImportType.class,
            MixedCommentImportType.class
        ).map(ImportCollectTest::fileToArgs);
    }

    @ParameterizedTest
    @MethodSource("fileProvider")
    public void testImports(Path path,
                            Class<?> sampleClass,
                            @ConvertWith(ImportMappingConverter.class) ImportMapping expected) throws IOException {
        final ImportNameCollector importCollector = new ImportNameCollector(ClassNamed.of(sampleClass), ClassResolver.atRuntime());
        collectImportsFrom(path, importCollector);

        String name = sampleClass.getSimpleName();

        ImportSet imports = importCollector.getImportMap().asSet(ImportCategory.TYPE);
        ImportSet expectedImports = expected.getImports();
        assertEquals(expectedImports.single(), imports.single(), "Regular imports doesn't match for " + name);
        assertEquals(expectedImports.global(), imports.global(), "Regular global imports doesn't match for " + name);

        ImportSet staticImports = importCollector.getImportMap().asSet(ImportCategory.STATIC);
        ImportSet expectedStaticImports = expected.getStaticImports();
        assertEquals(expectedStaticImports.single(), staticImports.single(), "Static imports doesn't match for " + name);
        assertEquals(expectedStaticImports.global(), staticImports.global(), "Static global imports doesn't match for " + name);
    }

    private static class ImportMappingConverter extends YamlMappingConverter<ImportMapping> {

        protected ImportMappingConverter() {
            super(ImportMapping.class);
        }
    }
}
