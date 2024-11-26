package io.papermc.typewriter.parser.sequence.hook;

import io.papermc.typewriter.parser.token.PrintableToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.util.WeakenConsumer;

public interface Hook {

    Hook pre(WeakenConsumer<PrintableToken, Token> callback);

    Hook post(WeakenConsumer<PrintableToken, Token> callback);
}
