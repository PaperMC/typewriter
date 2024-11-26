package io.papermc.typewriter.context;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.Optional;

/**
 * A file metadata for a specific source file registered through a {@link io.papermc.typewriter.registration.SourceSetRewriter}.
 *
 * @param header the import header
 * @param indentUnit the indent unit used
 */
@DefaultQualifier(NonNull.class)
public record FileMetadata(Optional<ImportLayout.Header> header, Optional<IndentUnit> indentUnit) {

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
     * Constructs a file metadata with a specified import header.
     *
     * @param header the import header
     * @return the file metadata
     */
    public static FileMetadata layout(ImportLayout.Header header) {
        return new FileMetadata(Optional.of(header), Optional.empty());
    }
}
