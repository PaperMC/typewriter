package io.papermc.typewriter.context;

public record ImportTypeName(String name, boolean isGlobal, boolean isStatic, boolean newlyAdded) implements Comparable<ImportTypeName> {

    @Override
    public int compareTo(ImportTypeName other) {
        return this.name.compareTo(other.name());
    }
}
