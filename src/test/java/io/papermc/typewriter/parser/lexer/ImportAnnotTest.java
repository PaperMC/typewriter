package io.papermc.typewriter.parser.lexer;

import io.papermc.typewriter.parser.Keywords;
import io.papermc.typewriter.parser.ParserTest;
import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;
import org.junit.jupiter.api.Test;

import static io.papermc.typewriter.parser.ParserAssertions.assertIdentifier;
import static io.papermc.typewriter.parser.ParserAssertions.assertNIdentifier;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ImportAnnotTest extends ParserTest {

    @Test
    public void testImport() {
        parseJava("""
                import a.b.c.d;
                """,
            lexer -> {
                CharSequenceToken importToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(importToken, 0, Keywords.IMPORT);

                assertNIdentifier(lexer, 4);

                Token secoToken = lexer.nextToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testStaticImport() {
        parseJava("""
                import static a.b.c.d;
                """,
            lexer -> {
                CharSequenceToken importToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(importToken, 0, Keywords.IMPORT);

                CharSequenceToken staticToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(staticToken, Keywords.IMPORT.length() + 1, Keywords.STATIC);

                assertNIdentifier(lexer, 4);

                Token secoToken = lexer.nextToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testGlobalImport() {
        parseJava("""
                import a.b.c.*;
                """,
            lexer -> {
                CharSequenceToken importToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(importToken, 0, Keywords.IMPORT);

                assertNIdentifier(lexer, 3);

                Token dotToken = lexer.nextToken();
                assertSame(TokenType.DOT, dotToken.type());

                Token starToken = lexer.nextToken();
                assertSame(TokenType.STAR, starToken.type());

                Token secoToken = lexer.nextToken();
                assertSame(TokenType.SECO, secoToken.type());
            });
    }

    @Test
    public void testStaticGlobalImport() {
        parseJava("""
                import static a.b.c.*;
                """,
            lexer -> {
                CharSequenceToken importToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(importToken, 0, Keywords.IMPORT);

                CharSequenceToken staticToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(staticToken, Keywords.IMPORT.length() + 1, Keywords.STATIC);

                assertNIdentifier(lexer, 3);

                Token dotToken = lexer.nextToken();
                assertSame(TokenType.DOT, dotToken.type());

                Token starToken = lexer.nextToken();
                assertSame(TokenType.STAR, starToken.type());

                Token secoToken = lexer.nextToken();
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
                CharSequenceToken publicToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(publicToken, 0, Keywords.PUBLIC);

                Token atToken = lexer.nextToken();
                assertSame(TokenType.AT_SIGN, atToken.type());

                CharSequenceToken interfaceToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(interfaceToken, Keywords.PUBLIC.length() + 1 + TokenType.AT_SIGN.value.length(), Keywords.INTERFACE);

                CharSequenceToken identifierToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken, Keywords.PUBLIC.length() + 1 + TokenType.AT_SIGN.value.length() + Keywords.INTERFACE.length() + 1, "Test");

                Token openBracket = lexer.nextToken();
                assertSame(TokenType.LSCOPE, openBracket.type());

                Token closeBracket = lexer.nextToken();
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
                Token atToken = lexer.nextToken();
                assertSame(TokenType.AT_SIGN, atToken.type());

                CharSequenceToken annotationNameToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(annotationNameToken, 1, "Test");

                CharSequenceToken publicToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(publicToken, 0, Keywords.PUBLIC);

                CharSequenceToken finalToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(finalToken, Keywords.PUBLIC.length() + 1, Keywords.FINAL);

                CharSequenceToken classToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(classToken, Keywords.PUBLIC.length() + 1 + Keywords.FINAL.length() + 1, "class");

                CharSequenceToken identifierToken = lexer.nextToken(CharSequenceToken.class);
                assertIdentifier(identifierToken, Keywords.PUBLIC.length() + 1 + Keywords.FINAL.length() + 1 + "class".length() + 1, "Api");

                Token openBracket = lexer.nextToken();
                assertSame(TokenType.LSCOPE, openBracket.type());

                Token closeBracket = lexer.nextToken();
                assertSame(TokenType.RSCOPE, closeBracket.type());
            });
    }
}
