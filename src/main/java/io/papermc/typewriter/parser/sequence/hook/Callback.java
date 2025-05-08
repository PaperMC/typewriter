package io.papermc.typewriter.parser.sequence.hook;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.parser.token.PrintableToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Consumer;

public record Callback(Consumer<PrintableToken> pre, Consumer<PrintableToken> post) {
    public static final Callback NO_OP = new Callback(token -> {}, token -> {});

    public static Callback partialOrComplete(@Nullable Consumer<PrintableToken> pre, @Nullable Consumer<PrintableToken> post) {
        Preconditions.checkArgument(pre != null || post != null);
        return new Callback(
            pre == null ? NO_OP.pre() : pre,
            post == null ? NO_OP.post() : post
        );
    }
}
