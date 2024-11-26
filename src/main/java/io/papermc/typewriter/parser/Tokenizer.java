package io.papermc.typewriter.parser;

import io.papermc.typewriter.parser.token.Token;

public interface Tokenizer {

    Token readToken();
}
