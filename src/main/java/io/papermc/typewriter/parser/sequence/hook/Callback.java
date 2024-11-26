package io.papermc.typewriter.parser.sequence.hook;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.parser.token.PrintableToken;
import io.papermc.typewriter.parser.token.Token;
import io.papermc.typewriter.util.WeakenConsumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public record Callback(WeakenConsumer<PrintableToken, Token> pre, WeakenConsumer<PrintableToken, Token> post) {
    public static final Callback NO_OP = new Callback(token -> {}, token -> {});

    public static Callback partialOrComplete(@Nullable WeakenConsumer<PrintableToken, Token> pre, @Nullable WeakenConsumer<PrintableToken, Token> post) {
        Preconditions.checkArgument(pre != null || post != null);
        return new Callback(
            pre == null ? NO_OP.pre() : pre,
            post == null ? NO_OP.post() : post
        );
    }
}
