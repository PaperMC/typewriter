package io.papermc.typewriter.parser.token;

public interface Token {

    TokenType type();

    Object value();

    Token UNKNOWN = new RawToken(TokenType.UNKNOWN, -1);
}
