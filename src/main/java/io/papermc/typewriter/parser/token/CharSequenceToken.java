package io.papermc.typewriter.parser.token;

import java.util.EnumSet;
import java.util.Set;

public record CharSequenceToken(TokenType type, String value, int row, int column, int endColumn, int pos, int endPos) implements PrintableToken {

    public int length() {
        return this.endColumn - this.column;
    }

    public static final Set<TokenType> TYPES = EnumSet.of(TokenType.CHAR, TokenType.STRING, TokenType.IDENTIFIER, TokenType.SINGLE_COMMENT);
}
