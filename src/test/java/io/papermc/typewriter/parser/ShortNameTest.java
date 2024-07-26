package io.papermc.typewriter.parser;

import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.context.ImportTypeCollector;
import io.papermc.typewriter.yaml.ImportShortNameMapping;
import io.papermc.typewriter.yaml.YamlMappingConverter;
import name.GlobalImportType;
import name.PackageClassImportType;
import name.RegularImportType;
import name.RemoteGlobalInnerClassImportType;
import name.RemoteInnerClassImportType;
import name.RemoteStaticGlobalInnerClassImportType;
import name.SelfInnerClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ShortNameTest extends ParserTest {

    private static Arguments rootClass(Class<?> sampleClass) {
        return innerClass(sampleClass, sampleClass);
    }

    private static Arguments innerClass(Class<?> sampleClass, Class<?> sampleInnerClass) {
        String name = sampleClass.getSimpleName();
        return Arguments.of(
            CONTAINER.resolve(sampleClass.getCanonicalName().replace('.', '/') + ".java"),
            sampleInnerClass,
            name,
            "expected/name/%s.yaml".formatted(sampleInnerClass.getName().substring(sampleInnerClass.getPackageName().length() + 1))
        );
    }

    private static Stream<Arguments> fileProvider() {
        return Stream.of(
            rootClass(RegularImportType.class),
            rootClass(GlobalImportType.class),
            rootClass(PackageClassImportType.class),
            rootClass(RemoteGlobalInnerClassImportType.class),
            rootClass(RemoteStaticGlobalInnerClassImportType.class),
            rootClass(RemoteInnerClassImportType.class),
            rootClass(SelfInnerClass.class),
            innerClass(SelfInnerClass.class, SelfInnerClass.A.B.C.class),
            innerClass(SelfInnerClass.class, SelfInnerClass.D.class),
            innerClass(SelfInnerClass.class, SelfInnerClass.A.B.E.class)
        );
    }

    @ParameterizedTest
    @MethodSource("fileProvider")
    public void testTypeName(Path path,
                             Class<?> sampleClass,
                             String name,
                             @ConvertWith(ImportShortNameMappingConverter.class) ImportShortNameMapping mapping) throws IOException {
        ClassNamed accessSource = new ClassNamed(sampleClass);
        final ImportTypeCollector importCollector = new ImportTypeCollector(accessSource.topLevel());
        importCollector.setAccessSource(accessSource);
        parseFile(path, importCollector);

        assertFalse(mapping.getShortNames() == null && mapping.getMemberShortNames() == null, "Empty expected import mapping!");

        if (mapping.getShortNames() != null) {
            for (Map.Entry<String, String> expect : mapping.getShortNames().entrySet()) {
                String typeName = expect.getKey();
                Class<?> runtimeClass = classOr(expect.getKey(), null);
                assertNotNull(runtimeClass, "Runtime class cannot be null for import " + typeName);
                assertEquals(expect.getValue(), importCollector.getShortName(runtimeClass),
                    "Short name of " + typeName + " doesn't match with collected imports for " + name + "! Import found: " + importCollector.getImports());
            }
        }

        if (mapping.getMemberShortNames() != null) {
            for (Map.Entry<String, String> expect : mapping.getMemberShortNames().entrySet()) {
                String fullName = expect.getKey();
                assertEquals(expect.getValue(), importCollector.getStaticMemberShortName(fullName),
                    "Short name of static member/class " + fullName + " doesn't match with collected imports for " + name + "! Static imports found: " + importCollector.getStaticImports());
            }
        }
    }

    private static Class<?> classOr(String className, Class<?> defaultClass) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return defaultClass;
        }
    }

    private static class ImportShortNameMappingConverter extends YamlMappingConverter<ImportShortNameMapping> {

        protected ImportShortNameMappingConverter() {
            super(ImportShortNameMapping.class);
        }
    }
}
