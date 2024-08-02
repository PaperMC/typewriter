package io.papermc.typewriter.parser;

import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

public class LexerTest extends ParserTest { // todo more test to cover import/annotation/nested annotation/closure

    private void assertNIdentifier(Lexer lexer, int count) {
        for (int i = 0; i < count; i++) {
            Token idToken = lexer.readToken();
            assertSame(TokenType.IDENTIFIER, idToken.type());

            if (i != count - 1) {
                Token dotToken = lexer.readToken();
                assertSame(TokenType.DOT, dotToken.type());
            }
        }
    }

    @Test
    public void testImport() {
        parseJava("""
                import a.b.c.d;
                """,
            lexer -> {
                Token importToken = lexer.readToken();
                assertSame(TokenType.IMPORT, importToken.type());
                assertSame(0, ((CharSequenceToken) importToken).startPos());
                assertSame(0 + TokenType.IMPORT.name.length(), ((CharSequenceToken) importToken).endPos());

                assertNIdentifier(lexer, 4);

                Token secoToken = lexer.readToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testStaticImport() {
        parseJava("""
                import static a.b.c.d;
                """,
            lexer -> {
                Token importToken = lexer.readToken();
                assertSame(TokenType.IMPORT, importToken.type());
                assertSame(0, ((CharSequenceToken) importToken).startPos());
                assertSame(0 + TokenType.IMPORT.name.length(), ((CharSequenceToken) importToken).endPos());

                int expectedStaticStartCursor = lexer.getCursor() + 1; // import token size + one space
                Token staticToken = lexer.readToken();
                assertSame(TokenType.STATIC, staticToken.type());
                assertSame(expectedStaticStartCursor, ((CharSequenceToken) staticToken).startPos());
                assertSame(expectedStaticStartCursor + TokenType.STATIC.name.length(), ((CharSequenceToken) staticToken).endPos());

                assertNIdentifier(lexer, 4);

                Token secoToken = lexer.readToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testGlobalImport() {
        parseJava("""
                import a.b.c.*;
                """,
            lexer -> {
                Token importToken = lexer.readToken();
                assertSame(TokenType.IMPORT, importToken.type());
                assertSame(0, ((CharSequenceToken) importToken).startPos());
                assertSame(0 + TokenType.IMPORT.name.length(), ((CharSequenceToken) importToken).endPos());

                assertNIdentifier(lexer, 3);

                Token dotToken = lexer.readToken();
                assertSame(TokenType.DOT, dotToken.type());

                Token starToken = lexer.readToken();
                assertSame(TokenType.STAR, starToken.type());

                Token secoToken = lexer.readToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testStaticGlobalImport() {
        parseJava("""
                import static a.b.c.*;
                """,
            lexer -> {
                Token importToken = lexer.readToken();
                assertSame(TokenType.IMPORT, importToken.type());
                assertSame(0, ((CharSequenceToken) importToken).startPos());
                assertSame(0 + TokenType.IMPORT.name.length(), ((CharSequenceToken) importToken).endPos());

                int expectedStaticStartCursor = lexer.getCursor() + 1; // import token size + one space
                Token staticToken = lexer.readToken();
                assertSame(TokenType.STATIC, staticToken.type());
                assertSame(expectedStaticStartCursor, ((CharSequenceToken) staticToken).startPos());
                assertSame(expectedStaticStartCursor + TokenType.STATIC.name.length(), ((CharSequenceToken) staticToken).endPos());

                assertNIdentifier(lexer, 3);

                Token dotToken = lexer.readToken();
                assertSame(TokenType.DOT, dotToken.type());

                Token starToken = lexer.readToken();
                assertSame(TokenType.STAR, starToken.type());

                Token secoToken = lexer.readToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }
}
