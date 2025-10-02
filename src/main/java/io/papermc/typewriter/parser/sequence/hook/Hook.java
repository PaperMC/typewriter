package io.papermc.typewriter.parser.sequence.hook;

import io.papermc.typewriter.parser.token.PrintableToken;

import java.util.function.Consumer;

public interface Hook {

    Hook pre(Consumer<PrintableToken> callback);

    Hook post(Consumer<PrintableToken> callback);
}
