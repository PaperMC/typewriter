package io.papermc.typewriter.parser;

import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.context.ImportCollector;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.PrintableToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import javax.lang.model.element.Modifier;
import org.junit.jupiter.api.Tag;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import static io.papermc.typewriter.parser.ParserAssertions.assertIdentifier;
import static io.papermc.typewriter.parser.ParserAssertions.assertKeyword;
import static io.papermc.typewriter.parser.ParserAssertions.assertNToken;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

@Tag("parser")
public class ParserTest {

    protected static final Set<JavaFeature> FEATURES = EnumSet.allOf(JavaFeature.class);
    protected static final Path CONTAINER = Path.of(System.getProperty("user.dir"), "src/testData/java");

    protected void collectImportsFrom(Path path, ImportCollector importCollector) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            ImportParser.collectImports(Lexer.fromReader(reader, FEATURES), importCollector, SourceFile.of(CONTAINER.relativize(path)));
        }
    }

    public interface TokenAccessor {

        default PrintableToken nextToken() {
            return this.nextToken(PrintableToken.class);
        }

        <T extends PrintableToken> T nextToken(Class<T> type);

        static TokenAccessor wrap(Tokenizer tokenizer) {
            return new TokenAccessor() {

                @Override
                public <T extends PrintableToken> T nextToken(Class<T> type) {
                    Token token = tokenizer.readToken();
                    assertNotSame(Token.END_OF_INPUT, token, "Reach end of stream");
                    assertInstanceOf(type, token, () -> "Expected a %s".formatted(type.getSimpleName()));
                    return type.cast(token);
                }
            };
        }
    }

    protected void parseJavaFile(Path path, Modifier classModifier, String classType, Consumer<TokenAccessor> callback) throws IOException {
        parseJavaFile(path, EnumSet.of(classModifier), classType, callback);
    }

    protected void parseJavaFile(Path path, EnumSet<Modifier> classModifiers, String classType, Consumer<TokenAccessor> callback) throws IOException {
        final Lexer lexer;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            lexer = Lexer.fromReader(reader, FEATURES);
        }

        TokenAccessor accessor = TokenAccessor.wrap(lexer);
        assertKeyword(accessor.nextToken(CharSequenceToken.class), 0, TokenType.PACKAGE);
        assertIdentifier(accessor.nextToken(CharSequenceToken.class), TokenType.PACKAGE.value.length() + 1, CONTAINER.relativize(path.getParent()).toString().replace('/', '.'));
        assertSame(TokenType.SECO, accessor.nextToken().type());

        int offset = 0;
        for (Modifier modifier : classModifiers) {
            String value = modifier.toString();
            assertIdentifier(accessor.nextToken(CharSequenceToken.class), offset, TokenType.fromValue(value, TokenType.IDENTIFIER), value);
            offset += value.length() + 1; // expect one space between tokens
        }

        assertIdentifier(accessor.nextToken(CharSequenceToken.class), offset, classType);
        String name = path.getFileName().toString();
        assertIdentifier(accessor.nextToken(CharSequenceToken.class), offset + classType.length() + 1, name.substring(0, name.length() - ".java".length()));

        assertNToken(accessor, TokenType.LSCOPE, 2);

        callback.accept(accessor);

        assertNToken(accessor, TokenType.RSCOPE, 2);
        assertSame(Token.END_OF_INPUT, lexer.readToken(), "Unexpected token found: not end of stream");
    }

    protected void parseJava(String content, Consumer<TokenAccessor> callback) {
        Lexer lexer = new Lexer(content.toCharArray(), FEATURES);
        TokenAccessor accessor = TokenAccessor.wrap(lexer);
        callback.accept(accessor);
        assertSame(Token.END_OF_INPUT, lexer.readToken(), "Unexpected token found: not end of stream");
    }
}
