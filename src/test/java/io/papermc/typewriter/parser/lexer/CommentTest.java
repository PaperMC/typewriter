package io.papermc.typewriter.parser.lexer;

import io.papermc.typewriter.parser.ParserTest;
import io.papermc.typewriter.parser.token.CharSequenceBlockToken;
import io.papermc.typewriter.parser.token.CharSequenceToken;
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
                CharSequenceToken commentToken = lexer.nextToken(CharSequenceToken.class);
                String expectedValue = " my comment";
                assertSame(TokenType.SINGLE_COMMENT, commentToken.type());
                assertSame(0, commentToken.column());
                assertSame(2 + expectedValue.length(), commentToken.endColumn());
                assertEquals(expectedValue, commentToken.value());
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
                    CharSequenceBlockToken commentToken = lexer.nextToken(CharSequenceBlockToken.class);
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(4 + 2 + 0 + 2, commentToken.endColumn());
                    assertSame(commentToken.endRow(), commentToken.row());
                    assertSame(1, commentToken.row());
                    assertEquals(List.of(), commentToken.value());
                }
                {
                    CharSequenceBlockToken commentToken = lexer.nextToken(CharSequenceBlockToken.class);
                    String expectedValue = "my comment!";
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(4 + 2 + 1 + expectedValue.length() + 1 + 2, commentToken.endColumn());
                    assertSame(commentToken.endRow(), commentToken.row());
                    assertSame(2, commentToken.row());
                    assertEquals(List.of(expectedValue), commentToken.value());
                }
                {
                    CharSequenceBlockToken commentToken = lexer.nextToken(CharSequenceBlockToken.class);
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(3, commentToken.row());
                    assertSame(4, commentToken.endRow());
                    assertEquals(List.of("my comment", "   !"), commentToken.value());
                }
                {
                    CharSequenceBlockToken commentToken = lexer.nextToken(CharSequenceBlockToken.class);
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(5, commentToken.row());
                    assertSame(8, commentToken.endRow());
                    assertEquals(List.of("    my comment", "!"), commentToken.value());
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
                    CharSequenceBlockToken javadocToken = lexer.nextToken(CharSequenceBlockToken.class);
                    String expectedValue = "my javadoc!";
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(4 + 3 + 1 + expectedValue.length() + 1 + 2, javadocToken.endColumn());
                    assertSame(javadocToken.endRow(), javadocToken.row());
                    assertSame(1, javadocToken.row());
                    assertEquals(List.of(expectedValue), javadocToken.value());
                }
                {
                    CharSequenceBlockToken javadocToken = lexer.nextToken(CharSequenceBlockToken.class);
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(2, javadocToken.row());
                    assertSame(3, javadocToken.endRow());
                    assertEquals(List.of("my javadoc", "!"), javadocToken.value());
                }
                {
                    CharSequenceBlockToken javadocToken = lexer.nextToken(CharSequenceBlockToken.class);
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(4, javadocToken.row());
                    assertSame(7, javadocToken.endRow());
                    assertEquals(List.of("my javadoc", "!"), javadocToken.value());
                }
                {
                    CharSequenceBlockToken javadocToken = lexer.nextToken(CharSequenceBlockToken.class);
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(8, javadocToken.row());
                    assertSame(11, javadocToken.endRow());
                    assertEquals(List.of("my javadoc", "!"), javadocToken.value());
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
                    CharSequenceBlockToken javadocToken = lexer.nextToken(CharSequenceBlockToken.class);
                    String expectedValue = "my javadoc!";
                    assertSame(TokenType.MARKDOWN_JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(4 + 3 + 1 + expectedValue.length(), javadocToken.endColumn());
                    assertSame(javadocToken.endRow(), javadocToken.row());
                    assertSame(1, javadocToken.row());
                    assertEquals(List.of(expectedValue), javadocToken.value());
                }
                {
                    CharSequenceBlockToken javadocToken = lexer.nextToken(CharSequenceBlockToken.class);
                    String expectedValue = "my javadoc!";
                    assertSame(TokenType.MARKDOWN_JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(4 + 3 + 1 + expectedValue.length() + 2, javadocToken.endColumn());
                    assertSame(javadocToken.endRow(), javadocToken.row());
                    assertSame(3, javadocToken.row());
                    assertEquals(List.of(expectedValue), javadocToken.value());
                }
                {
                    CharSequenceBlockToken javadocToken = lexer.nextToken(CharSequenceBlockToken.class);
                    assertSame(TokenType.MARKDOWN_JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(5, javadocToken.row());
                    assertSame(6, javadocToken.endRow());
                    assertEquals(List.of("my javadoc", "!"), javadocToken.value());
                }
            });
    }
}
