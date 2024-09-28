package io.papermc.typewriter.context;

import org.checkerframework.checker.nullness.qual.NonNull;

public record ImportTypeName(String name, boolean isGlobal, boolean isStatic, boolean newlyAdded) implements Comparable<ImportTypeName> {

    @Override
    public int compareTo(@NonNull ImportTypeName other) {
        return this.name.compareTo(other.name());
    }
}
