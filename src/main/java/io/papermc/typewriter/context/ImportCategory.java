package io.papermc.typewriter.context;

import io.papermc.typewriter.parser.Keywords;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.Optional;
import java.util.function.Function;

@DefaultQualifier(NonNull.class)
public class ImportCategory<T extends ImportName> {

    public static final ImportCategory<ImportName.Type> TYPE = new ImportCategory<>(ImportName.Type::fromQualifiedName, null);
    public static final ImportCategory<ImportName.Static> STATIC = new ImportCategory<>(ImportName.Static::fromQualifiedMemberName, Keywords.STATIC);

    private final Function<String, T> fromUnsafeName;
    private final Optional<String> identity;

    private ImportCategory(Function<String, T> fromUnsafeName, @Nullable String identity) {
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
