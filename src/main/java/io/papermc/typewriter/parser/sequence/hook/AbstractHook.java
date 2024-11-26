package io.papermc.typewriter.parser.sequence.hook;

import io.papermc.typewriter.parser.token.PrintableToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.util.WeakenConsumer;

public class AbstractHook implements Hook {

    private WeakenConsumer<PrintableToken, Token> pre;
    private WeakenConsumer<PrintableToken, Token> post;

    @Override
    public Hook pre(WeakenConsumer<PrintableToken, Token> callback) {
        this.pre = callback;
        return this;
    }

    @Override
    public Hook post(WeakenConsumer<PrintableToken, Token> callback) {
        this.post = callback;
        return this;
    }

    public Callback callback() {
        return Callback.partialOrComplete(this.pre, this.post);
    }
}
