package io.papermc.typewriter.parser.lexer;

import io.papermc.typewriter.parser.ParserTest;
import io.papermc.typewriter.parser.token.CharSequenceBlockToken;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class StringTest extends ParserTest {

    @Test
    public void testRegularString() {
        parseJava("""
                String abc = "regular string";
                """,
            lexer -> {
                Token identifierToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken, 0, TokenType.IDENTIFIER, "String");

                Token identifierToken2 = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken2, "String".length() + 1, TokenType.IDENTIFIER, "abc");

                Token stringToken = lexer.readToken();
                String expectedValue = "regular string";
                assertSame(TokenType.STRING, stringToken.type());
                assertSame("String".length() + 1 + "abc".length() + 3, stringToken.column());
                assertSame("String".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1, ((CharSequenceToken) stringToken).endColumn());
                assertEquals(expectedValue, ((CharSequenceToken) stringToken).value());

                Token secoToken = lexer.readToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testStringEscape() throws IOException {
        parseJava(CONTAINER.resolve("lexer/StringEscapes.java"), 8, lexer -> {
            Token identifierToken = lexer.readToken();
            assertIdentifier((CharSequenceToken) identifierToken, 8, TokenType.IDENTIFIER, "String");

            Token identifierToken2 = lexer.readToken();
            assertIdentifier((CharSequenceToken) identifierToken2, 8 + "String".length() + 1, TokenType.IDENTIFIER, "abc");

            Token stringToken = lexer.readToken();
            String expectedValue = "\tsome \"string\"\\\n";
            assertSame(TokenType.STRING, stringToken.type());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3, stringToken.column());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1 + 5, ((CharSequenceToken) stringToken).endColumn());
            assertEquals(expectedValue, ((CharSequenceToken) stringToken).value());

            Token secoToken = lexer.readToken();
            assertSame(TokenType.SECO, secoToken.type());
        });
    }

    @Test
    public void testSurrogatePair() throws IOException {
        parseJava(CONTAINER.resolve("lexer/SurrogatePair.java"), 8, lexer -> {
            Token identifierToken = lexer.readToken();
            assertIdentifier((CharSequenceToken) identifierToken, 8, TokenType.IDENTIFIER, "String");

            Token identifierToken2 = lexer.readToken();
            assertIdentifier((CharSequenceToken) identifierToken2, 8 + "String".length() + 1, TokenType.IDENTIFIER, "abc");

            Token stringToken = lexer.readToken();
            String expectedValue = "𝄞𝄞";
            assertSame(TokenType.STRING, stringToken.type());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3, stringToken.column());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1, ((CharSequenceToken) stringToken).endColumn());
            assertEquals(expectedValue, ((CharSequenceToken) stringToken).value());

            Token secoToken = lexer.readToken();
            assertSame(TokenType.SECO, secoToken.type());
        });
    }

    @Test
    public void testRegularChar() {
        parseJava("""
                char abc = 'a';
                """,
            lexer -> {
                Token identifierToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken, 0, TokenType.IDENTIFIER, "char");

                Token identifierToken2 = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken2, "char".length() + 1, TokenType.IDENTIFIER, "abc");

                Token charToken = lexer.readToken();
                String expectedValue = "a";
                assertSame(TokenType.CHAR, charToken.type());
                assertSame("char".length() + 1 + "abc".length() + 3, charToken.column());
                assertSame("char".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1, ((CharSequenceToken) charToken).endColumn());
                assertEquals(expectedValue, ((CharSequenceToken) charToken).value());

                Token secoToken = lexer.readToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testCharEscape() {
        parseJava("""
                char abc = '\'';
                """,
            lexer -> {
                Token identifierToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken, 0, TokenType.IDENTIFIER, "char");

                Token identifierToken2 = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken2, "char".length() + 1, TokenType.IDENTIFIER, "abc");

                Token charToken = lexer.readToken();
                String expectedValue = "'";
                assertSame(TokenType.CHAR, charToken.type());
                assertSame("char".length() + 1 + "abc".length() + 3, charToken.column());
                assertSame("char".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1, ((CharSequenceToken) charToken).endColumn());
                assertEquals(expectedValue, ((CharSequenceToken) charToken).value());

                Token secoToken = lexer.readToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testTextBlock() {
        parseJava("""
                String abc = \"""
                        Test
                    !
                    \""";
                """,
            lexer -> {
                Token identifierToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken, 0, TokenType.IDENTIFIER, "String");

                Token identifierToken2 = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken2, "String".length() + 1, TokenType.IDENTIFIER, "abc");

                Token textBlockToken = lexer.readToken();
                assertSame(TokenType.PARAGRAPH, textBlockToken.type());
                assertSame(1, textBlockToken.row());
                assertSame(4, ((CharSequenceBlockToken) textBlockToken).endRow());
                assertEquals(List.of("    Test", "!", ""), ((CharSequenceBlockToken) textBlockToken).value());

                Token secoToken = lexer.readToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testTextBlockEscapes() throws IOException {
        parseJava(CONTAINER.resolve("lexer/TextBlockEscapes.java"), 8,
            lexer -> {
                Token identifierToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken, 8, TokenType.IDENTIFIER, "String");

                Token identifierToken2 = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken2, 8 + "String".length() + 1, TokenType.IDENTIFIER, "abc");

                Token textBlockToken = lexer.readToken();
                assertSame(TokenType.PARAGRAPH, textBlockToken.type());
                assertSame(6, textBlockToken.row());
                assertSame(11, ((CharSequenceBlockToken) textBlockToken).endRow());
                assertEquals(List.of("Test", "Test 2    ", " long line", ""), ((CharSequenceBlockToken) textBlockToken).value());

                Token secoToken = lexer.readToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }
}
