package io.papermc.typewriter.context;

import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public enum ImportCategory {
    TYPE(ImportName.Type.class::isInstance),
    STATIC(ImportName.Static.class::isInstance);

    private final Predicate<ImportName> filter;

    ImportCategory(Predicate<ImportName> filter) {
        this.filter = filter;
    }

    public ImportLayout.PackageFilter allOther() {
        return ImportLayout.PackageFilter.wrap(this.filter);
    }

    public ImportLayout.PackageFilter startsWith(String prefix) {
        return ImportLayout.PackageFilter.wrap(this.filter.and(type -> type.name().startsWith(prefix)));
    }

    public ImportLayout.PackageFilter endsWith(String suffix) {
        return ImportLayout.PackageFilter.wrap(this.filter.and(type -> type.name().endsWith(suffix)));
    }

    public ImportLayout.PackageFilter matches(Supplier<Pattern> regex) { // supplier must be a memoizer or a constant
        return ImportLayout.PackageFilter.wrap(this.filter.and(type -> regex.get().matcher(type.name()).find()));
    }
}
