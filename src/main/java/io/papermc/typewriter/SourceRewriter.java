package io.papermc.typewriter;

import io.papermc.typewriter.context.SourcesMetadata;
import io.papermc.typewriter.util.ClassNamedView;
import io.papermc.typewriter.util.ClassResolver;

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
     * @param metadata the sources metadata
     * @param resolver the class resolver
     * @param view the class named view
     * @param file the source file
     * @throws IOException if an I/O error occur
     */
    void writeToFile(Path parent, SourcesMetadata metadata, ClassResolver resolver, ClassNamedView view, SourceFile file) throws IOException;
}
