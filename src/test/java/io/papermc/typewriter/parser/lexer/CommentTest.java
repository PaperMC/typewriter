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
                    String expectedValue = "my comment!";
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(4 + 2 + 1 + expectedValue.length() + 1 + 2, ((CharSequenceBlockToken) commentToken).endColumn());
                    assertSame(((CharSequenceBlockToken) commentToken).endRow(), commentToken.row());
                    assertSame(1, commentToken.row());
                    assertEquals(List.of(expectedValue), ((CharSequenceBlockToken) commentToken).value());
                }
                {
                    Token commentToken = lexer.readToken();
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(2, commentToken.row());
                    assertSame(3, ((CharSequenceBlockToken) commentToken).endRow());
                    assertEquals(List.of("my comment", "   !"), ((CharSequenceBlockToken) commentToken).value());
                }
                {
                    Token commentToken = lexer.readToken();
                    assertSame(TokenType.COMMENT, commentToken.type());
                    assertSame(4, commentToken.column());
                    assertSame(4, commentToken.row());
                    assertSame(7, ((CharSequenceBlockToken) commentToken).endRow());
                    assertEquals(List.of("    my comment", "!"), ((CharSequenceBlockToken) commentToken).value());
                }
            });
    }

    @Test
    public void testJavadoc() { // todo move /**/ as an empty multi line comment
        parseJava("""
                /**/
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
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(4 + 2 + 0 + 2, ((CharSequenceBlockToken) javadocToken).endColumn());
                    assertSame(((CharSequenceBlockToken) javadocToken).endRow(), javadocToken.row());
                    assertSame(1, javadocToken.row());
                    assertEquals(List.of(), ((CharSequenceBlockToken) javadocToken).value());
                }
                {
                    Token javadocToken = lexer.readToken();
                    String expectedValue = "my javadoc! "; // todo trailing space should be trimmed
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(4 + 3 + 1 + expectedValue.length() + 2, ((CharSequenceBlockToken) javadocToken).endColumn());
                    assertSame(((CharSequenceBlockToken) javadocToken).endRow(), javadocToken.row());
                    assertSame(2, javadocToken.row());
                    assertEquals(List.of(expectedValue), ((CharSequenceBlockToken) javadocToken).value());
                }
                {
                    Token javadocToken = lexer.readToken();
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(3, javadocToken.row());
                    assertSame(4, ((CharSequenceBlockToken) javadocToken).endRow());
                    assertEquals(List.of("my javadoc", "! "), ((CharSequenceBlockToken) javadocToken).value());
                }
                {
                    Token javadocToken = lexer.readToken();
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(5, javadocToken.row());
                    assertSame(8, ((CharSequenceBlockToken) javadocToken).endRow());
                    assertEquals(List.of("my javadoc", "!"), ((CharSequenceBlockToken) javadocToken).value());
                }
                {
                    Token javadocToken = lexer.readToken();
                    assertSame(TokenType.JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(9, javadocToken.row());
                    assertSame(12, ((CharSequenceBlockToken) javadocToken).endRow());
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
                    String expectedValue = " my javadoc!";
                    assertSame(TokenType.MARKDOWN_JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(0, ((CharSequenceBlockToken) javadocToken).endColumn()); // todo shouldn't be zero 4 + 3 + 1 + expectedValue.length()
                    // todo: assertSame(((CharSequenceBlockToken) javadocToken).endRow(), javadocToken.row());
                    assertSame(1, javadocToken.row());
                    assertEquals(List.of(expectedValue), ((CharSequenceBlockToken) javadocToken).value());
                }
                {
                    Token javadocToken = lexer.readToken();
                    String expectedValue = " my javadoc!  "; // todo trim
                    assertSame(TokenType.MARKDOWN_JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(0, ((CharSequenceBlockToken) javadocToken).endColumn()); // 4 + 3 + 1 + expectedValue.length()
                    // assertSame(((CharSequenceBlockToken) javadocToken).endRow(), javadocToken.row());
                    assertSame(3, javadocToken.row());
                    assertEquals(List.of(expectedValue), ((CharSequenceBlockToken) javadocToken).value());
                }
                /* EOI eat the last token for mjd todo
                {
                    Token javadocToken = lexer.readToken();
                    assertSame(TokenType.MARKDOWN_JAVADOC, javadocToken.type());
                    assertSame(4, javadocToken.column());
                    assertSame(5, javadocToken.row());
                    //assertSame(6, ((CharSequenceBlockToken) javadocToken).endRow());
                    assertEquals(List.of(" my javadoc", " !"), ((CharSequenceBlockToken) javadocToken).value());
                }*/
            });
    }
}
