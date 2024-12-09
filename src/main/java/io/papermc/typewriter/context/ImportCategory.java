package io.papermc.typewriter.context;

import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

public class ImportCategory<T extends ImportName> {

    public static final ImportCategory<ImportName.Type> TYPE = new ImportCategory<>(ImportName.Type.class, null);

    public static final ImportCategory<ImportName.Static> STATIC = new ImportCategory<>(ImportName.Static.class, "static");

    @ApiStatus.Experimental
    public static final ImportCategory<ImportName.Module> MODULE = new ImportCategory<>(ImportName.Module.class, "module");

    private final Class<T> klazz;
    private final Optional<String> identity;

    private ImportCategory(Class<T> klazz, String identity) {
        this.klazz = klazz;
        this.identity = Optional.ofNullable(identity);
    }

    public Optional<String> identity() {
        return this.identity;
    }
}
