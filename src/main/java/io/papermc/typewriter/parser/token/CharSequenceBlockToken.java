package io.papermc.typewriter.parser.token;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record CharSequenceBlockToken(TokenType type, List<String> value, int row, int column, int startPos, int endPos) implements Token {

    public static final Set<TokenType> TYPES = EnumSet.of(TokenType.PARAGRAPH, TokenType.COMMENT, TokenType.JAVADOC, TokenType.MARKDOWN_JAVADOC);
}
