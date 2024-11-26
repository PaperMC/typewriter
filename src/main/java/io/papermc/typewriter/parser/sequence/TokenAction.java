package io.papermc.typewriter.parser.sequence;

import io.papermc.typewriter.parser.token.Token;

public interface TokenAction {

    boolean execute(Token token, SequenceTokens executor);
}
