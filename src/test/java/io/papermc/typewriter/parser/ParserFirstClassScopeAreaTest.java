package io.papermc.typewriter.parser;

import area.AnnotationClass;
import area.AnnotationPresentClass;
import area.AnnotationTrapClass;
import area.FancyNewlineAnnotationPresentClass;
import area.FancyScopeClass;
import area.FancyScopeClass2;
import area.MixedAnnotationPresentClass;
import area.NearScopeClass;
import area.NewlineScopedClass;
import area.SimpleTrapClass;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.context.ImportTypeCollector;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ParserFirstClassScopeAreaTest extends ParserTest {

    private static Arguments fileToArgs(Class<?> sampleClass) {
        String name = sampleClass.getSimpleName();
        return Arguments.of(
            CONTAINER.resolve(sampleClass.getCanonicalName().replace('.', '/') + ".java"),
            sampleClass,
            name
        );
    }

    private static Stream<Arguments> fileProvider() {
        return Stream.of(
            SimpleTrapClass.class,
            AnnotationClass.class,
            AnnotationPresentClass.class,
            AnnotationTrapClass.class,
            FancyNewlineAnnotationPresentClass.class,
            MixedAnnotationPresentClass.class,
            NewlineScopedClass.class,
            NearScopeClass.class,
            FancyScopeClass.class,
            FancyScopeClass2.class
        ).map(ParserFirstClassScopeAreaTest::fileToArgs);
    }

    private static final Pattern EXPECTED_LINE = Pattern.compile("<< (?<cursor>\\d+?)$");

    @ParameterizedTest
    @MethodSource("fileProvider")
    public void testFirstClassScope(Path path,
                            Class<?> sampleClass,
                            String name) throws IOException {
        final ImportTypeCollector importCollector = new ImportTypeCollector(new ClassNamed(sampleClass));

        parseFile(path, importCollector, line -> {
            String textLine = line.getString();
            Matcher matcher = EXPECTED_LINE.matcher(textLine);
            if (matcher.find()) {
                int cursor = Integer.parseInt(matcher.group("cursor"));
                assertEquals(cursor, line.getCursor(), "Parser didn't stop at the expected cursor for " + name);
            } else {
                fail("Parser didn't stop at the expected line, for " + name + "! found: " + textLine);
            }
        },
        () -> {
            fail("File is empty or doesn't contains the required top scope needed for this test to run");
        });
    }
}
