package io.papermc.typewriter.context;

import io.papermc.typewriter.parser.Keywords;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;
import java.util.function.Function;

public class ImportCategory<T extends ImportName> {

    public static final ImportCategory<ImportName.Type> TYPE = new ImportCategory<>(ImportName.Type::fromQualifiedName, null);
    public static final ImportCategory<ImportName.Static> STATIC = new ImportCategory<>(ImportName.Static::fromQualifiedMemberName, Keywords.STATIC);
    @ApiStatus.Experimental
    public static final ImportCategory<ImportName.Module> MODULE = new ImportCategory<>(ImportName.Module::fromQualifiedName, Keywords.MODULE);

    private final Function<String, T> fromUnsafeName;
    private final Optional<String> identity;

    private ImportCategory(Function<String, T> fromUnsafeName, String identity) {
        this.fromUnsafeName = fromUnsafeName;
        this.identity = Optional.ofNullable(identity);
    }

    T parse(String name) {
        return this.fromUnsafeName.apply(name);
    }

    public Optional<String> identity() {
        return this.identity;
    }
}
