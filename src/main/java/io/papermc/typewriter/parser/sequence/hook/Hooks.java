package io.papermc.typewriter.parser.sequence.hook;

import java.util.function.UnaryOperator;

public interface Hooks {

    void bind(HookType type, UnaryOperator<Hook> callback);
}
