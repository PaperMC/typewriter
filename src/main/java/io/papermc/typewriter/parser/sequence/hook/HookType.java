package io.papermc.typewriter.parser.sequence.hook;

import io.papermc.typewriter.parser.token.PrintableToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.util.WeakenConsumer;

public enum HookType {
    FIRST {
        private static final Hook EMPTY_HOOK = new AbstractHook() {
            @Override
            public Hook post(WeakenConsumer<PrintableToken, Token> callback) {
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
            public Hook pre(WeakenConsumer<PrintableToken, Token> callback) {
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
