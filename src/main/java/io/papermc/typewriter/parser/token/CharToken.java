package io.papermc.typewriter.parser.token;

public record CharToken(TokenType type, Character value, int row, int column, int pos, int endPos) implements PrintableToken {

}
