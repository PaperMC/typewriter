package io.papermc.typewriter.context.layout;

import java.util.function.UnaryOperator;

public record ImportHeader(ImportScheme scheme, ImportOrder order) {

    public static final ImportHeader DEFAULT = inlined(scheme -> scheme
        .group(PackageFilter.TYPE)
        .newline()
        .group(PackageFilter.STATIC)
        .newline()
        .group(PackageFilter.MODULE), ImportOrder.MERGED);

    private static ImportHeader inlined(UnaryOperator<ImportScheme> scheme, ImportOrder order) {
        return new ImportHeader(scheme.apply(new ImportScheme()), order);
    }
}
