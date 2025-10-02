package io.papermc.typewriter.context.layout;

import io.papermc.typewriter.context.ImportCategory;
import io.papermc.typewriter.context.ImportName;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.function.Predicate;

@DefaultQualifier(NonNull.class)
public final class ImportFilter {

    public static final Predicate<ImportName> TYPE = only(ImportCategory.TYPE);
    public static final Predicate<ImportName> STATIC = only(ImportCategory.STATIC);
    public static final Predicate<ImportName> ONLY_WILDCARD = type -> type instanceof ImportName.Identified identified && identified.isWildcard();

    private static Predicate<ImportName> only(ImportCategory<?> category) {
        return type -> type.category() == category;
    }

    private ImportFilter() {
    }
}
