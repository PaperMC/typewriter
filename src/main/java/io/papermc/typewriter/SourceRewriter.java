package io.papermc.typewriter;

import io.papermc.typewriter.context.SourcesMetadata;

import java.io.IOException;
import java.nio.file.Path;

public interface SourceRewriter {

    /**
     * Callback when a source rewriter is registered into a source set.
     *
     * @param file the source file
     * @return {@code true} to register this rewriter
     */
    default boolean registerFor(SourceFile file) {
        return true;
    }

    /**
     * Apply this rewriter to a source file
     *
     * @param parent the parent of the source file (a source set path)
     * @param file   the source file
     * @throws IOException if an I/O error occur
     */
    default void writeToFile(Path parent, SourcesMetadata metadata, SourceFile file) throws IOException {
        writeToFile(parent, parent, metadata, file);
    }

    /**
     * Only used by Paper for new rewriter use {@link #writeToFile(Path, SourcesMetadata, SourceFile)}
     */
    @Deprecated // used only for paper test
    void writeToFile(Path readFolder, Path writeFolder, SourcesMetadata metadata, SourceFile file) throws IOException;
}
