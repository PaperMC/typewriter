package io.papermc.typewriter.context;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

/**
 * A file metadata for all source files registered through a {@link io.papermc.typewriter.registration.SourceSetRewriter}.
 *
 * @param importLayout the import layout used
 * @param indentUnit the default indent unit used if no one is found in the source file
 */
@DefaultQualifier(NonNull.class)
public record SourcesMetadata(ImportLayout importLayout, IndentUnit indentUnit) {

    /**
     * Constructs a file metadata with the default import layout and
     * a specified indent unit.
     *
     * @param indentUnit the indent unit
     * @return the file metadata
     */
    public static SourcesMetadata of(IndentUnit indentUnit) {
        return new SourcesMetadata(ImportLayout.DEFAULT, indentUnit);
    }
}
