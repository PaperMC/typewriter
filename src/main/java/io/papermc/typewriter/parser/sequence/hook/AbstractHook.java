package io.papermc.typewriter.parser.sequence.hook;

import io.papermc.typewriter.parser.token.PrintableToken;

import java.util.function.Consumer;

public class AbstractHook implements Hook {

    private Consumer<PrintableToken> pre;
    private Consumer<PrintableToken> post;

    @Override
    public Hook pre(Consumer<PrintableToken> callback) {
        this.pre = callback;
        return this;
    }

    @Override
    public Hook post(Consumer<PrintableToken> callback) {
        this.post = callback;
        return this;
    }

    public Callback callback() {
        return Callback.partialOrComplete(this.pre, this.post);
    }
}
