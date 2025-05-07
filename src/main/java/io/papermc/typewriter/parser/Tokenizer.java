package io.papermc.typewriter.parser;

import io.papermc.typewriter.parser.token.Token;

import java.util.Set;

public interface Tokenizer {

    Token readToken();

    Set<JavaFeature> getFeatures();
}
