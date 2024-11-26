package io.papermc.typewriter.parser;

import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.PrintableToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.junit.jupiter.api.Tag;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

@Tag("parser")
public class ParserTest {

    protected static final Path CONTAINER = Path.of(System.getProperty("user.dir"), "src/testData/java");

    protected void collectImportsFrom(Path path, ImportCollector importCollector) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ImportParser.collectImports(Lexer.fromReader(reader), importCollector, SourceFile.of(CONTAINER.relativize(path)));
        }
    }

    protected interface TokenAccessor {

        PrintableToken nextToken();

        <T extends Token> T nextToken(Class<T> type);

        static TokenAccessor wrap(Tokenizer tokenizer) {
            return new TokenAccessor() {
                @Override
                public PrintableToken nextToken() {
                    Token token = tokenizer.readToken();
                    assertNotSame(TokenType.EOI, token.type(), "Reach end of stream");
                    assertInstanceOf(PrintableToken.class, token, "Expected a printable token");
                    return (PrintableToken) token;
                }

                @Override
                public <T extends Token> T nextToken(Class<T> type) {
                    Token token = tokenizer.readToken();
                    assertNotSame(TokenType.EOI, token.type(), "Reach end of stream");
                    assertInstanceOf(type, token, () -> "Expected a %s".formatted(type.getSimpleName()));
                    return type.cast(token);
                }
            };
        }
    }

    protected void parseJava(Path path, int skipTokens, Consumer<TokenAccessor> callback) throws IOException {
        final TokenAccessor getter;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            getter = TokenAccessor.wrap(Lexer.fromReader(reader));
        }
        while (skipTokens > 0) {
            getter.nextToken();
            skipTokens--;
        }
        callback.accept(getter);
    }

    protected void parseJava(String content, Consumer<TokenAccessor> callback) {
        callback.accept(TokenAccessor.wrap(new Lexer(content.toCharArray())));
    }

    protected void assertNIdentifier(TokenAccessor lexer, int count) {
        for (int i = 0; i < count; i++) {
            Token idToken = lexer.nextToken();
            assertSame(TokenType.IDENTIFIER, idToken.type());

            if (i != count - 1) {
                Token dotToken = lexer.nextToken();
                assertSame(TokenType.DOT, dotToken.type());
            }
        }
    }

    protected void assertKeyword(CharSequenceToken token, int offset, TokenType type) {
        assertIdentifier(token, offset, type, Objects.requireNonNull(token.type().value));
    }

    protected void assertIdentifier(CharSequenceToken token, int offset, TokenType expectedType, String expectedValue) {
        assertSame(expectedType, token.type());
        assertSame(offset, token.column());
        assertSame(offset + expectedValue.length(), token.endColumn());
    }
}
