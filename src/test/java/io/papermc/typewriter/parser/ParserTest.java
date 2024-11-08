package io.papermc.typewriter.parser;

import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Tag;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertSame;

@Tag("parser")
public class ParserTest {

    protected static final Path CONTAINER = Path.of(System.getProperty("user.dir"), "src/testData/java");

    protected void parseFile(Path path, ImportCollector importCollector) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ImportParser.collectImports(Lexer.fromReader(reader), importCollector);
        }
    }

    protected void parseJava(Path path, int skipTokens, Consumer<Lexer> callback) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            var lex = Lexer.fromReader(reader);
            while (skipTokens > 0) {
                lex.readToken();
                skipTokens--;
            }
            callback.accept(lex);
        }
    }

    protected void parseJava(String content, Consumer<Lexer> callback) {
        callback.accept(new Lexer(content.toCharArray()));
    }

    protected void assertNIdentifier(Lexer lexer, int count) {
        for (int i = 0; i < count; i++) {
            Token idToken = lexer.readToken();
            assertSame(TokenType.IDENTIFIER, idToken.type());

            if (i != count - 1) {
                Token dotToken = lexer.readToken();
                assertSame(TokenType.DOT, dotToken.type());
            }
        }
    }

    protected void assertIdentifier(CharSequenceToken token, int offset, TokenType type) {
        assertIdentifier(token, offset, type, null);
    }

    protected void assertIdentifier(CharSequenceToken token, int offset, TokenType expectedType, @Nullable String expectedValue) {
        assertSame(expectedType, token.type());
        assertSame(offset, token.column());

        if (expectedValue == null) {
            expectedValue = token.type().name;
        }
        assertSame(offset + Objects.requireNonNull(expectedValue).length(), token.endColumn());
    }
}
