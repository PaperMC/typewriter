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
            Token identifierToken = lexer.readToken();
            assertIdentifier((CharSequenceToken) identifierToken, 8, TokenType.IDENTIFIER, "String");

            Token identifierToken2 = lexer.readToken();
            assertIdentifier((CharSequenceToken) identifierToken2, 8 + "String".length() + 1, TokenType.IDENTIFIER, "abc");

            Token stringToken = lexer.readToken();
            String expectedValue = "some string";
            assertSame(TokenType.STRING, stringToken.type());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3, stringToken.column());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1, ((CharSequenceToken) stringToken).endColumn());
            assertEquals(expectedValue, ((CharSequenceToken) stringToken).value());

            Token secoToken = lexer.readToken();
            assertSame(TokenType.SECO, secoToken.type());
        });
    }
}
