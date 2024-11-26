package io.papermc.typewriter.parser.sequence;

import io.papermc.typewriter.parser.sequence.hook.Hooks;

import java.util.function.Consumer;

public interface TokenTaskBuilder {

    TokenTaskBuilder asOptional();

    TokenTaskBuilder asRepeatable();

    TokenTaskBuilder hooks(Consumer<Hooks> hooks);
}
