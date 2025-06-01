package io.papermc.typewriter.preset;

import io.papermc.typewriter.preset.model.EnumConstant;

import java.util.EnumSet;

public class EnumCloneRewriter<T extends Enum<T>> extends EnumRewriter<T> { // not really a clone anymore

    private final Class<T> basedOn;

    public EnumCloneRewriter(Class<T> basedOn) {
        this.basedOn = basedOn;
        this.reachEnd = true;
    }

    @Override
    protected Iterable<T> getValues() {
        return EnumSet.allOf(this.basedOn);
    }

    @Override
    protected EnumConstant.Builder constantPrototype(T value) {
        return EnumConstant.builder(value.name());
    }
}
