package io.papermc.typewriter.registration;

import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.SourceRewriter;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface SourceSetRewriter<T extends SourceSetRewriter<T>> {

    @VisibleForTesting
    Map<SourceFile, SourceRewriter> getRewriters();

    T register(SourceFile source, SourceRewriter rewriter);

    void apply(Path output) throws IOException;
}
