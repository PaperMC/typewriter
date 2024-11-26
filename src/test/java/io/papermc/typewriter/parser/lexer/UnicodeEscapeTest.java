package io.papermc.typewriter.parser.lexer;

import io.papermc.typewriter.parser.ParserTest;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class UnicodeEscapeTest extends ParserTest {

    @Test
    public void testString() throws IOException {
        parseJava(CONTAINER.resolve("lexer/UnicodeEscapes.java"), 8, lexer -> {
            CharSequenceToken identifierToken = lexer.nextToken(CharSequenceToken.class);
            assertIdentifier(identifierToken, 8, TokenType.IDENTIFIER, "String");

            CharSequenceToken identifierToken2 = lexer.nextToken(CharSequenceToken.class);
            assertIdentifier(identifierToken2, 8 + "String".length() + 1, TokenType.IDENTIFIER, "abc");

            CharSequenceToken stringToken = lexer.nextToken(CharSequenceToken.class);
            String expectedValue = "some string";
            assertSame(TokenType.STRING, stringToken.type());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3, stringToken.column());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1, stringToken.endColumn());
            assertEquals(expectedValue, stringToken.value());

            Token secoToken = lexer.nextToken();
            assertSame(TokenType.SECO, secoToken.type());
        });
    }
}
