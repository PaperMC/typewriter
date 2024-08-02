package io.papermc.typewriter.parser.token;

import java.util.EnumSet;
import java.util.Set;

public record CharSequenceToken(TokenType type, String value, int startPos, int endPos) implements Token {

    public int length() {
        return this.endPos - this.startPos;
    }

    public static final Set<TokenType> TYPES = EnumSet.of(TokenType.CHAR, TokenType.STRING, TokenType.IDENTIFIER, TokenType.SINGLE_COMMENT);
}
