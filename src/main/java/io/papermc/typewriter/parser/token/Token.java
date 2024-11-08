package io.papermc.typewriter.parser.token;

public sealed interface Token permits RawToken, CharSequenceToken, CharSequenceBlockToken {

    TokenType type();

    Object value();

    int row();

    int column();

    int pos();
}
