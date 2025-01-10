package io.papermc.typewriter.registration;

import io.papermc.typewriter.context.SourcesMetadata;
import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.SourceRewriter;
import io.papermc.typewriter.util.ClassNamedView;
import io.papermc.typewriter.util.ClassResolver;

import java.io.IOException;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SourceSetRewriterImpl<T extends SourceSetRewriter<T>> implements SourceSetRewriter<T> {

    protected final Map<SourceFile, SourceRewriter> rewrites = new LinkedHashMap<>();
    protected final SourcesMetadata metadata;

    public SourceSetRewriterImpl(SourcesMetadata metadata) {
        this.metadata = metadata;
    }

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
        final ClassResolver resolver = this.makeResolver(this.metadata.classpath());
        final ClassNamedView view = new ClassNamedView(output, 20, null);
        for (Map.Entry<SourceFile, SourceRewriter> rewriter : this.rewrites.entrySet()) {
            rewriter.getValue().writeToFile(output, this.metadata, resolver, view, rewriter.getKey());
        }
    }

    private ClassResolver makeResolver(Set<Path> classpath) {
        if (classpath.isEmpty()) {
            return NoopClassResolver.INSTANCE;
        }

        return new ClassResolver(classpath);
    }

    private static class NoopClassResolver extends ClassResolver {
        public static final ClassResolver INSTANCE = new NoopClassResolver();

        public NoopClassResolver() {
            super(Collections.emptySet());
        }

        @Override
        public Optional<Class<?>> tryFind(String name) {
            return Optional.empty();
        }

        @Override
        public Optional<ModuleReference> tryFindModule(String name) {
            return Optional.empty();
        }
    }
}
