package io.papermc.typewriter.parser.token;

public record RawToken(TokenType type, int row, int column, int pos) implements Token {

    @Override
    public Object value() {
        throw new UnsupportedOperationException();
    }
}
