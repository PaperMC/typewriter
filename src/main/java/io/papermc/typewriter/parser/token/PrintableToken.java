package io.papermc.typewriter.parser.token;

public sealed interface PrintableToken extends Token permits CharToken, CharSequenceToken, CharSequenceBlockToken {

    Object value();

    int row();

    int column();

    int pos();
}
