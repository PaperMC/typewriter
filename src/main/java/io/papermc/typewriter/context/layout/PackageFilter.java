package io.papermc.typewriter.context.layout;

import io.papermc.typewriter.context.ImportCategory;
import io.papermc.typewriter.context.ImportName;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Predicate;

public class PackageFilter implements Predicate<ImportName> {

    public static final PackageFilter TYPE = only(ImportCategory.TYPE);

    public static final PackageFilter STATIC = only(ImportCategory.STATIC);

    @ApiStatus.Experimental
    public static final PackageFilter MODULE = only(ImportCategory.MODULE);

    private final Predicate<ImportName> filter;

    private PackageFilter(Predicate<ImportName> filter) {
        this.filter = filter;
    }

    public PackageFilter startsWith(String prefix) {
        return PackageFilter.wrap(this.filter.and(type -> type.name().startsWith(prefix)));
    }

    public PackageFilter endsWith(String suffix) {
        return PackageFilter.wrap(this.filter.and(type -> type.name().endsWith(suffix)));
    }

    public PackageFilter onlyWildcard() {
        return PackageFilter.wrap(this.filter.and(type -> type instanceof ImportName.Identified identified && identified.isGlobal()));
    }

    private static PackageFilter wrap(Predicate<ImportName> filter) {
        return new PackageFilter(filter);
    }

    private static PackageFilter only(ImportCategory<?> category) {
        return wrap(type -> type.category() == category);
    }

    @Override
    public boolean test(ImportName name) {
        return this.filter.test(name);
    }
}
