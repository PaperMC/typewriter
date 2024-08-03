package io.papermc.typewriter.parser.token;

public interface Token {

    TokenType type();

    Object value();

    int row();

    int column();
}
