package io.papermc.typewriter.parser;

import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.context.ImportCategory;
import io.papermc.typewriter.context.ImportNameCollector;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        String name = sampleClass.getSimpleName();
        final ImportNameCollector importCollector = new ImportNameCollector(ClassNamed.of(sampleClass));
        collectImportsFrom(path, importCollector);

        assertFalse(mapping.getShortNames() == null && mapping.getMemberShortNames() == null, "Empty expected import mapping!");

        if (mapping.getShortNames() != null) {
            for (Map.Entry<String, String> expect : mapping.getShortNames().entrySet()) {
                String typeName = expect.getKey();
                Class<?> runtimeClass = assertDoesNotThrow(() -> Class.forName(typeName), "Runtime class is unknown for import " + typeName);
                assertEquals(expect.getValue(), importCollector.getShortName(runtimeClass, false),
                    () -> "Short name of " + typeName + " doesn't match with collected imports for " + name + "! Import found: " + importCollector.getImportMap().get(ImportCategory.TYPE));
            }
        }

        Map<String, String> memberShortNames = mapping.getMemberShortNames();
        if (memberShortNames != null) {
            for (Map.Entry<String, String> expect : memberShortNames.entrySet()) {
                String expectedName = expect.getKey();
                String[] names = expectedName.split(":", 2);
                assertSame(2, names.length);
                assertEquals(expect.getValue(), importCollector.getStaticMemberShortName(names[0], names[1]),
                    () -> "Short name of static member/class " + expectedName + " doesn't match with collected imports for " + name + "! Static imports found: " + importCollector.getImportMap().get(ImportCategory.STATIC));
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

        ClassNamed from = ClassNamed.of(sampleClass);
        for (Map.Entry<String, String> expect : mapping.getShortNames().entrySet()) {
            String typeName = expect.getKey();
            Class<?> runtimeClass = assertDoesNotThrow(() -> Class.forName(typeName), "Runtime class is unknown for " + typeName);
            assertEquals(expect.getValue(), from.relativize(ClassNamed.of(runtimeClass)),
                "Shortest access name of " + typeName + " doesn't match with the expected name from " + from.canonicalName() + " !");
        }
    }

    private static class ShortNameMappingConverter extends YamlMappingConverter<ShortNameMapping> {

        protected ShortNameMappingConverter() {
            super(ShortNameMapping.class);
        }
    }
}
