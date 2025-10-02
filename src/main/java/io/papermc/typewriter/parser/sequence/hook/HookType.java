package io.papermc.typewriter.parser.sequence.hook;

import io.papermc.typewriter.parser.token.PrintableToken;

import java.util.function.Consumer;

public enum HookType {
    FIRST {
        private static final Hook EMPTY_HOOK = new AbstractHook() {
            @Override
            public Hook post(Consumer<PrintableToken> callback) {
                throw new UnsupportedOperationException("First hook cannot have a post token callback");
            }
        };

        @Override
        Hook emptyHook() {
            return EMPTY_HOOK;
        }
    },
    EVERY,
    LAST {
        private static final Hook EMPTY_HOOK = new AbstractHook() {
            @Override
            public Hook pre(Consumer<PrintableToken> callback) {
                throw new UnsupportedOperationException("Last hook cannot have a pre token callback");
            }
        };

        @Override
        Hook emptyHook() {
            return EMPTY_HOOK;
        }
    };

    private static final Hook EMPTY_HOOK = new AbstractHook();

    Hook emptyHook() {
        return EMPTY_HOOK;
    }
}
