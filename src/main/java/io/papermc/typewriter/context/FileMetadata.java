package io.papermc.typewriter.context;

import io.papermc.typewriter.context.layout.ImportLayout;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.Optional;

/**
 * A file metadata for a specific source file registered through a {@link io.papermc.typewriter.registration.SourceSetRewriter}.
 *
 * @param layout the import layout
 * @param indentUnit the indent unit used
 */
@DefaultQualifier(NonNull.class)
public record FileMetadata(Optional<ImportLayout> layout, Optional<IndentUnit> indentUnit) {

    /**
     * Constructs a file metadata with a specified indent unit.
     *
     * @param indentUnit the indent unit
     * @return the file metadata
     */
    public static FileMetadata indent(IndentUnit indentUnit) {
        return new FileMetadata(Optional.empty(), Optional.of(indentUnit));
    }

    /**
     * Constructs a file metadata with a specified import layout.
     *
     * @param layout the import layout
     * @return the file metadata
     */
    public static FileMetadata layout(ImportLayout layout) {
        return new FileMetadata(Optional.of(layout), Optional.empty());
    }
}
