package io.papermc.typewriter.parser.lexer;

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
import io.papermc.typewriter.parser.ParserTest;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class LexerTopClassScopeAreaTest extends ParserTest {

    private static Arguments fileToArgs(Class<?> sampleClass) {
        return Arguments.of(
            CONTAINER.resolve(sampleClass.getCanonicalName().replace('.', '/') + ".java"),
            sampleClass
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
        ).map(LexerTopClassScopeAreaTest::fileToArgs);
    }

    @ParameterizedTest
    @MethodSource("fileProvider")
    public void testFirstClassScope(Path path,
                                    Class<?> sampleClass) throws IOException {
        final ImportTypeCollector importCollector = new ImportTypeCollector(new ClassNamed(sampleClass));

        parseFile(path, importCollector, (lex, token) -> {
            Token nextToken = lex.readToken();
            assertSame(TokenType.SINGLE_COMMENT, nextToken.type());
            assertEquals(Integer.parseInt(((CharSequenceToken) nextToken).value().stripLeading()), token.pos());
        });
    }
}
