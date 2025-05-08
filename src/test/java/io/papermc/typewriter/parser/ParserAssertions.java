package io.papermc.typewriter.parser;

import io.papermc.typewriter.parser.token.CharSequenceToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.parser.token.TokenType;

import static org.junit.jupiter.api.Assertions.assertSame;

public final class ParserAssertions {

    private ParserAssertions() {
    }

    public static void assertNIdentifier(ParserTest.TokenAccessor accessor, int count) {
        for (int i = 0; i < count; i++) {
            Token idToken = accessor.nextToken();
            assertSame(TokenType.IDENTIFIER, idToken.type());

            if (i != count - 1) {
                Token dotToken = accessor.nextToken();
                assertSame(TokenType.DOT, dotToken.type());
            }
        }
    }

    public static void assertNToken(ParserTest.TokenAccessor accessor, TokenType type, int count) {
        for (int i = 0; i < count; i++) {
            Token token = accessor.nextToken();
            assertSame(type, token.type());
        }
    }

    public static void assertIdentifier(CharSequenceToken token, int offset, String expectedValue) {
        assertIdentifier(token, offset, TokenType.IDENTIFIER, expectedValue);
    }

    public static void assertIdentifier(CharSequenceToken token, int offset, TokenType expectedType, String expectedValue) {
        assertSame(expectedType, token.type());
        assertSame(offset, token.column());
        assertSame(offset + expectedValue.length(), token.endColumn());
    }
}
