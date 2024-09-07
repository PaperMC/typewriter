package io.papermc.typewriter.parser.lexer;

import io.papermc.typewriter.parser.ParserTest;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

public class ImportAnnotTest extends ParserTest {

    @Test
    public void testImport() {
        parseJava("""
                import a.b.c.d;
                """,
            lexer -> {
                Token importToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) importToken, 0, TokenType.IMPORT);

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
                assertIdentifier((CharSequenceToken) importToken, 0, TokenType.IMPORT);

                {
                    int expectedStaticStartCursor = lexer.getCursor() + 1; // import token size + one space
                    Token staticToken = lexer.readToken();
                    assertIdentifier((CharSequenceToken) staticToken, expectedStaticStartCursor, TokenType.STATIC);
                }

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
                assertIdentifier((CharSequenceToken) importToken, 0, TokenType.IMPORT);

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
                assertIdentifier((CharSequenceToken) importToken, 0, TokenType.IMPORT);

                {
                    int expectedStaticStartCursor = lexer.getCursor() + 1; // import token size + one space
                    Token staticToken = lexer.readToken();
                    assertIdentifier((CharSequenceToken) staticToken, expectedStaticStartCursor, TokenType.STATIC);
                }

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
    public void testAnnotationDeclaration() {
        parseJava("""
            public @interface Test {
            }
            """,
            lexer -> {
                Token publicToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) publicToken, 0, TokenType.PUBLIC);

                Token atToken = lexer.readToken();
                assertSame(TokenType.AT_SIGN, atToken.type());

                Token interfaceToken = lexer.readToken();
                assertSame(TokenType.INTERFACE, interfaceToken.type());

                Token identifierToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken, TokenType.PUBLIC.name.length() + 1 + 1 + TokenType.INTERFACE.name.length() + 1, TokenType.IDENTIFIER, "Test");

                Token openBracket = lexer.readToken();
                assertSame(TokenType.LSCOPE, openBracket.type());

                Token closeBracket = lexer.readToken();
                assertSame(TokenType.RSCOPE, closeBracket.type());
            });
    }

    @Test
    public void testAnnotation() {
        parseJava("""
                @Test
                public final class Api {
                }
                """,
            lexer -> {
                Token atToken = lexer.readToken();
                assertSame(TokenType.AT_SIGN, atToken.type());

                Token annotationNameToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) annotationNameToken, 1, TokenType.IDENTIFIER, "Test");

                Token publicToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) publicToken, 0, TokenType.PUBLIC);

                Token finalToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) finalToken, TokenType.PUBLIC.name.length() + 1, TokenType.FINAL);

                lexer.readToken(); // skip class token which is not recognized

                Token identifierToken = lexer.readToken();
                assertIdentifier((CharSequenceToken) identifierToken, TokenType.PUBLIC.name.length() + 1 + TokenType.FINAL.name.length() + 1 + "class".length() + 1, TokenType.IDENTIFIER, "Api");

                Token openBracket = lexer.readToken();
                assertSame(TokenType.LSCOPE, openBracket.type());

                Token closeBracket = lexer.readToken();
                assertSame(TokenType.RSCOPE, closeBracket.type());
            });
    }
}
