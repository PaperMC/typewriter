package io.papermc.typewriter.parser.lexer;

import io.papermc.typewriter.parser.ParserTest;
import io.papermc.typewriter.parser.token.CharSequenceBlockToken;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import javax.lang.model.element.Modifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static io.papermc.typewriter.parser.ParserAssertions.assertIdentifier;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class StringTest extends ParserTest {

    @Test
    public void testRegularString() {
        parseJava("""
                String abc = "regular string";
                """,
            lexer -> {
                CharSequenceToken identifierToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken, 0, "String");

                CharSequenceToken identifierToken2 = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken2, "String".length() + 1, "abc");

                CharSequenceToken stringToken = lexer.nextToken(CharSequenceToken.class);
                String expectedValue = "regular string";
                assertSame(TokenType.STRING, stringToken.type());
                assertSame("String".length() + 1 + "abc".length() + 3, stringToken.column());
                assertSame("String".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1, stringToken.endColumn());
                assertEquals(expectedValue, stringToken.value());

                Token secoToken = lexer.nextToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testStringEscape() throws IOException {
        parseJavaFile(CONTAINER.resolve("lexer/StringEscapes.java"), Modifier.PUBLIC, "class", lexer -> {
            CharSequenceToken identifierToken = lexer.nextToken(CharSequenceToken.class);
            assertIdentifier(identifierToken, 8, "String");

            CharSequenceToken identifierToken2 = lexer.nextToken(CharSequenceToken.class);
            assertIdentifier(identifierToken2, 8 + "String".length() + 1, "abc");

            CharSequenceToken stringToken = lexer.nextToken(CharSequenceToken.class);
            String expectedValue = "\tsome \"string\"\\\n";
            assertSame(TokenType.STRING, stringToken.type());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3, stringToken.column());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1 + 5, stringToken.endColumn());
            assertEquals(expectedValue, stringToken.value());

            Token secoToken = lexer.nextToken();
            assertSame(TokenType.SECO, secoToken.type());
        });
    }

    @Test
    public void testSurrogatePair() throws IOException {
        parseJavaFile(CONTAINER.resolve("lexer/SurrogatePair.java"), Modifier.PUBLIC, "class", lexer -> {
            CharSequenceToken identifierToken = lexer.nextToken(CharSequenceToken.class);
            assertIdentifier(identifierToken, 8, "String");

            CharSequenceToken identifierToken2 = lexer.nextToken(CharSequenceToken.class);
            assertIdentifier(identifierToken2, 8 + "String".length() + 1, "abc");

            CharSequenceToken stringToken = lexer.nextToken(CharSequenceToken.class);
            String expectedValue = "𝄞𝄞";
            assertSame(TokenType.STRING, stringToken.type());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3, stringToken.column());
            assertSame(8 + "String".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1, stringToken.endColumn());
            assertEquals(expectedValue, stringToken.value());

            Token secoToken = lexer.nextToken();
            assertSame(TokenType.SECO, secoToken.type());
        });
    }

    @Test
    public void testRegularChar() {
        parseJava("""
                char abc = 'a';
                """,
            lexer -> {
                CharSequenceToken identifierToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken, 0, "char");

                CharSequenceToken identifierToken2 = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken2, "char".length() + 1, "abc");

                CharSequenceToken charToken = lexer.nextToken(CharSequenceToken.class);
                String expectedValue = "a";
                assertSame(TokenType.CHAR, charToken.type());
                assertSame("char".length() + 1 + "abc".length() + 3, charToken.column());
                assertSame("char".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1, charToken.endColumn());
                assertEquals(expectedValue, charToken.value());

                Token secoToken = lexer.nextToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testCharEscape() {
        parseJava("""
                char abc = '\'';
                """,
            lexer -> {
                CharSequenceToken identifierToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken, 0, "char");

                CharSequenceToken identifierToken2 = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken2, "char".length() + 1, "abc");

                String expectedValue = "'";
                CharSequenceToken charToken = lexer.nextToken(CharSequenceToken.class);
                assertSame(TokenType.CHAR, charToken.type());
                assertSame("char".length() + 1 + "abc".length() + 3, charToken.column());
                assertSame("char".length() + 1 + "abc".length() + 3 + 1 + expectedValue.length() + 1, charToken.endColumn());
                assertEquals(expectedValue, charToken.value());

                Token secoToken = lexer.nextToken();
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
                CharSequenceToken identifierToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken, 0, "String");

                CharSequenceToken identifierToken2 = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken2, "String".length() + 1, "abc");

                CharSequenceBlockToken textBlockToken = lexer.nextToken(CharSequenceBlockToken.class);
                assertSame(TokenType.PARAGRAPH, textBlockToken.type());
                assertSame(1, textBlockToken.row());
                assertSame(4, textBlockToken.endRow());
                assertEquals(List.of("    Test", "!", ""), textBlockToken.value());

                Token secoToken = lexer.nextToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testTextBlockEscapes() throws IOException {
        parseJavaFile(CONTAINER.resolve("lexer/TextBlockEscapes.java"), Modifier.PUBLIC, "class",
            lexer -> {
                CharSequenceToken identifierToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken, 8, "String");

                CharSequenceToken identifierToken2 = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken2, 8 + "String".length() + 1, "abc");

                CharSequenceBlockToken textBlockToken = lexer.nextToken(CharSequenceBlockToken.class);
                assertSame(TokenType.PARAGRAPH, textBlockToken.type());
                assertSame(6, textBlockToken.row());
                assertSame(11, textBlockToken.endRow());
                assertEquals(List.of("Test", "Test 2    ", " long line", ""), textBlockToken.value());

                Token secoToken = lexer.nextToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }
}
