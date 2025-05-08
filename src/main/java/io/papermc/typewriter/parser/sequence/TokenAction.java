package io.papermc.typewriter.parser.sequence;

import io.papermc.typewriter.parser.token.PrintableToken;

public interface TokenAction {

    boolean execute(PrintableToken token, SequenceTokens executor);
}
