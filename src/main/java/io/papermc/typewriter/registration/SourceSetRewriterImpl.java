package io.papermc.typewriter.registration;

import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.SourceRewriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SourceSetRewriterImpl<T extends SourceSetRewriter<T>> implements SourceSetRewriter<T> {

    protected final Map<SourceFile, SourceRewriter> rewrites = new HashMap<>();

    @Override
    public Map<SourceFile, SourceRewriter> getRewriters() {
        return Collections.unmodifiableMap(this.rewrites);
    }

    @Override
    public T register(SourceFile source, SourceRewriter rewriter) {
        if (rewriter.registerFor(source)) {
            this.rewrites.put(source, rewriter);
        }
        return (T) this;
    }

    @Override
    public void apply(Path output) throws IOException {
        for (Map.Entry<SourceFile, SourceRewriter> rewriter : this.rewrites.entrySet()) {
            rewriter.getValue().writeToFile(output, rewriter.getKey());
        }
    }
}
