package io.papermc.typewriter.parser.lexer;

import io.papermc.typewriter.parser.ParserTest;
import io.papermc.typewriter.parser.token.CharSequenceBlockToken;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class CommentTest extends ParserTest {

    @Test
    public void testSingeLineComment() {
        parseJava("""
            // my comment
            """,
            lexer -> {
                Token commentToken = lexer.readToken();
                String expectedValue = " my comment";
                assertSame(TokenType.SINGLE_COMMENT, commentToken.type());
                assertSame(0, commentToken.column());
                assertSame(2 + expectedValue.length(), ((CharSequenceToken) commentToken).endColumn());
                assertEquals(expectedValue, ((CharSequenceToken) commentToken).value());
            });
    }

    @Test
    public void testComment() {
        parseJava("""
                /**/
                /* my comment! */
                /* my comment
                ! */
                /*
                my comment
            !
                */
            """,
            lexer -> {
                {
                    Token commentToken = lexer.readToken();
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(4 + 2 + 0 + 2, ((CharSequenceBlockToken) commentToken).endColumn());
                    assertSame(((CharSequenceBlockToken) commentToken).endRow(), commentToken.row());
                    assertSame(1, commentToken.row());
                    assertEquals(List.of(), ((CharSequenceBlockToken) commentToken).value());
                }
                {
                    Token commentToken = lexer.readToken();
                    String expectedValue = "my comment!";
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(4 + 2 + 1 + expectedValue.length() + 1 + 2, ((CharSequenceBlockToken) commentToken).endColumn());
                    assertSame(((CharSequenceBlockToken) commentToken).endRow(), commentToken.row());
                    assertSame(2, commentToken.row());
                    assertEquals(List.of(expectedValue), ((CharSequenceBlockToken) commentToken).value());
                }
                {
                    Token commentToken = lexer.readToken();
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(3, commentToken.row());
                    assertSame(4, ((CharSequenceBlockToken) commentToken).endRow());
                    assertEquals(List.of("my comment", "   !"), ((CharSequenceBlockToken) commentToken).value());
                }
                {
                    Token commentToken = lexer.readToken();
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(5, commentToken.row());
                    assertSame(8, ((CharSequenceBlockToken) commentToken).endRow());
                    assertEquals(List.of("    my comment", "!"), ((CharSequenceBlockToken) commentToken).value());
                }
            });
    }

    @Test
    public void testJavadoc() {
        parseJava("""
                /** my javadoc! */
                /** my javadoc
                ! */
                /**
                 * my javadoc
                 * !
                 */
                /**
                * my javadoc
                * !
                */
            """,
            lexer -> {
                {
                    Token javadocToken = lexer.readToken();
                    String expectedValue = "my javadoc!";
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(4 + 3 + 1 + expectedValue.length() + 1 + 2, ((CharSequenceBlockToken) javadocToken).endColumn());
                    assertSame(((CharSequenceBlockToken) javadocToken).endRow(), javadocToken.row());
                    assertSame(1, javadocToken.row());
                    assertEquals(List.of(expectedValue), ((CharSequenceBlockToken) javadocToken).value());
                }
                {
                    Token javadocToken = lexer.readToken();
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(2, javadocToken.row());
                    assertSame(3, ((CharSequenceBlockToken) javadocToken).endRow());
                    assertEquals(List.of("my javadoc", "!"), ((CharSequenceBlockToken) javadocToken).value());
                }
                {
                    Token javadocToken = lexer.readToken();
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(4, javadocToken.row());
                    assertSame(7, ((CharSequenceBlockToken) javadocToken).endRow());
                    assertEquals(List.of("my javadoc", "!"), ((CharSequenceBlockToken) javadocToken).value());
                }
                {
                    Token javadocToken = lexer.readToken();
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(8, javadocToken.row());
                    assertSame(11, ((CharSequenceBlockToken) javadocToken).endRow());
                    assertEquals(List.of("my javadoc", "!"), ((CharSequenceBlockToken) javadocToken).value());
                }
            });
    }

    @Test
    public void testMarkdownJavadoc() {
        parseJava("""
                    /// my javadoc!

                    /// my javadoc! \s

                    /// my javadoc
                    /// !
                """,
            lexer -> {
                {
                    Token javadocToken = lexer.readToken();
                    String expectedValue = "my javadoc!";
                    assertSame(TokenType.MARKDOWN_JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(4 + 3 + 1 + expectedValue.length(), ((CharSequenceBlockToken) javadocToken).endColumn());
                    assertSame(((CharSequenceBlockToken) javadocToken).endRow(), javadocToken.row());
                    assertSame(1, javadocToken.row());
                    assertEquals(List.of(expectedValue), ((CharSequenceBlockToken) javadocToken).value());
                }
                {
                    Token javadocToken = lexer.readToken();
                    String expectedValue = "my javadoc!";
                    assertSame(TokenType.MARKDOWN_JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(4 + 3 + 1 + expectedValue.length() + 2, ((CharSequenceBlockToken) javadocToken).endColumn());
                    assertSame(((CharSequenceBlockToken) javadocToken).endRow(), javadocToken.row());
                    assertSame(3, javadocToken.row());
                    assertEquals(List.of(expectedValue), ((CharSequenceBlockToken) javadocToken).value());
                }
                {
                    Token javadocToken = lexer.readToken();
                    assertSame(TokenType.MARKDOWN_JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(5, javadocToken.row());
                    assertSame(6, ((CharSequenceBlockToken) javadocToken).endRow());
                    assertEquals(List.of("my javadoc", "!"), ((CharSequenceBlockToken) javadocToken).value());
                }
            });
    }
}
