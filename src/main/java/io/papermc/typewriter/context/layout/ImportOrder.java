package io.papermc.typewriter.context.layout;

import io.papermc.typewriter.context.ImportName;

import java.util.Comparator;

/**
 * The import order inside a group of import statements.
 */
public enum ImportOrder {

    /**
     * Both generated imports and original imports will be merged and
     * sorted alphabetically uniformly.
     */
    MERGED(Comparator.naturalOrder()),

    /**
     * Generated imports will be written first followed by the original imports in the class.
     * Each part are ordered alphabetically independently.
     */
    GENERATED_FIRST(Comparator.comparing(ImportName::newlyAdded).reversed().thenComparing(Comparator.naturalOrder())),

    /**
     * Generated imports will be written last after the original imports in the class.
     * Each part are ordered alphabetically independently.
     */
    GENERATED_LAST(Comparator.comparing(ImportName::newlyAdded).thenComparing(Comparator.naturalOrder()));

    public final Comparator<ImportName> comparator;

    ImportOrder(Comparator<ImportName> comparator) {
        this.comparator = comparator;
    }
}
