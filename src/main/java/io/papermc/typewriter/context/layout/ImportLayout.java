package io.papermc.typewriter.context.layout;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.List;
import java.util.function.UnaryOperator;

@DefaultQualifier(NonNull.class)
public final class ImportLayout {

    public static final ImportLayout DEFAULT = inlined(scheme -> scheme
        .group(ImportFilter.TYPE)
        .newline()
        .group(ImportFilter.STATIC), ImportOrder.MERGED);

    private final List<ImportScheme.Item> scheme;
    private final ImportOrder order;

    private ImportLayout(List<ImportScheme.Item> scheme, ImportOrder order) {
        this.scheme = scheme;
        this.order = order;
    }

    public static ImportLayout inlined(UnaryOperator<ImportScheme> scheme, ImportOrder order) {
        return new ImportLayout(scheme.apply(new ImportScheme()).view(), order);
    }

    public List<ImportScheme.Item> scheme() {
        return this.scheme;
    }

    public ImportOrder order() {
        return this.order;
    }
}
