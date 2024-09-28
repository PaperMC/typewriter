package io.papermc.typewriter;

/**
 * A file metadata for all source files registered through a {@link io.papermc.typewriter.registration.SourceSetRewriter}.
 *
 * @param importLayout the import layout used
 * @param indentUnit the default indent unit used if no one is found in the source file
 */
public record FileMetadata(ImportLayout importLayout, IndentUnit indentUnit) {

    /**
     * Constructs a file metadata with the default import layout and
     * a specified indent unit.
     *
     * @param indentUnit the indent unit
     * @return the file metadata
     */
    public static FileMetadata of(IndentUnit indentUnit) {
        return new FileMetadata(ImportLayout.DEFAULT, indentUnit);
    }
}
