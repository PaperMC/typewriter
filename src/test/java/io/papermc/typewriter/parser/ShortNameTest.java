package io.papermc.typewriter.parser;

import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.context.ImportTypeCollector;
import io.papermc.typewriter.yaml.ShortNameMapping;
import io.papermc.typewriter.yaml.YamlMappingConverter;
import name.GlobalImportType;
import name.PackageClassImportType;
import name.RegularImportType;
import name.RemoteGlobalNestedClassImportType;
import name.RemoteNestedClassImportType;
import name.RemoteGlobalStaticNestedClassImportType;
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
        return Arguments.of(
            CONTAINER.resolve(sampleClass.getCanonicalName().replace('.', '/') + ".java"),
            sampleClass,
            "expected/name/%s.yaml".formatted(sampleClass.getName().substring(sampleClass.getPackageName().length() + 1))
        );
    }

    private static Stream<Arguments> importTypeSamples() {
        return Stream.of(
            rootClass(RegularImportType.class),
            rootClass(GlobalImportType.class),
            rootClass(PackageClassImportType.class),
            rootClass(RemoteGlobalNestedClassImportType.class),
            rootClass(RemoteGlobalStaticNestedClassImportType.class),
            rootClass(RemoteNestedClassImportType.class)
        );
    }

    @ParameterizedTest
    @MethodSource("importTypeSamples")
    public void testTypeName(Path path,
                             Class<?> sampleClass,
                             @ConvertWith(ShortNameMappingConverter.class) ShortNameMapping mapping) throws IOException {
        ClassNamed accessSource = new ClassNamed(sampleClass);
        String name = sampleClass.getSimpleName();
        final ImportTypeCollector importCollector = new ImportTypeCollector(accessSource.topLevel());
        importCollector.setAccessSource(accessSource);
        parseFile(path, importCollector);

        assertFalse(mapping.getShortNames() == null && mapping.getMemberShortNames() == null, "Empty expected import mapping!");

        if (mapping.getShortNames() != null) {
            for (Map.Entry<String, String> expect : mapping.getShortNames().entrySet()) {
                String typeName = expect.getKey();
                Class<?> runtimeClass = classOr(typeName, null);
                assertNotNull(runtimeClass, "Runtime class cannot be null for import " + typeName);
                assertEquals(expect.getValue(), importCollector.getShortName(runtimeClass, false),
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

    private static Arguments innerClass(Class<?> sampleClass) {
        return Arguments.of(
            sampleClass,
            "expected/name/%s.yaml".formatted(sampleClass.getName().substring(sampleClass.getPackageName().length() + 1))
        );
    }

    public static Stream<Arguments> innerTypeSamples() {
        return Stream.of(
            innerClass(SelfInnerClass.class),
            innerClass(SelfInnerClass.A.B.C.class),
            innerClass(SelfInnerClass.D.class),
            innerClass(SelfInnerClass.A.B.E.class)
        );
    }

    @ParameterizedTest
    @MethodSource("innerTypeSamples")
    public void testInnerTypeName(Class<?> sampleClass,
                                  @ConvertWith(ShortNameMappingConverter.class) ShortNameMapping mapping) {
        assertNotNull(mapping.getShortNames(), "Empty name mapping!");

        ClassNamed from = new ClassNamed(sampleClass);
        for (Map.Entry<String, String> expect : mapping.getShortNames().entrySet()) {
            String typeName = expect.getKey();
            Class<?> runtimeClass = classOr(typeName, null);
            assertNotNull(runtimeClass, "Runtime class cannot be null for import " + typeName);
            assertEquals(expect.getValue(), from.relativize(new ClassNamed(runtimeClass)),
                "Shortest access name of " + typeName + " doesn't match with the expected name from " + from.canonicalName() + " !");
        }
    }

    private static Class<?> classOr(String className, Class<?> defaultClass) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            return defaultClass;
        }
    }

    private static class ShortNameMappingConverter extends YamlMappingConverter<ShortNameMapping> {

        protected ShortNameMappingConverter() {
            super(ShortNameMapping.class);
        }
    }
}
