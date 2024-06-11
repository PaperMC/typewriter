package io.papermc.typewriter.replace;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.SourceFile;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A composite rewriter used to tie multiple rewriters (of type {@link SearchReplaceRewriter})
 * together for a single java source file.
 */
@DefaultQualifier(NonNull.class)
public class CompositeRewriter extends SearchReplaceRewriterBase {

    private final Set<SearchReplaceRewriter> rewriters;

    private CompositeRewriter(Set<SearchReplaceRewriter> rewriters) {
        this.rewriters = rewriters;
    }

    @Override
    public boolean registerFor(SourceFile file) {
        boolean result = true;
        for (SearchReplaceRewriter rewriter : this.rewriters) {
            if (!rewriter.registerFor(file)) {
                result = false; // continue to iterate to show next errors (if any)
            }
        }
        return result;
    }

    @Contract(value = "_ -> new", pure = true)
    public static CompositeRewriter bind(SearchReplaceRewriter... rewriters) {
        return bind(Arrays.asList(rewriters));
    }

    @Contract(value = "_ -> new", pure = true)
    public static CompositeRewriter bind(Collection<SearchReplaceRewriter> rewriters) {
        Preconditions.checkArgument(!rewriters.isEmpty(), "Rewriter list cannot be empty!");

        Set<String> startCommentMarkers = new HashSet<>(rewriters.size());
        Set<String> endCommentMarkers = new HashSet<>(rewriters.size());

        for (SearchReplaceRewriter rewriter : rewriters) {
            Preconditions.checkArgument(rewriter.options != null, "Replace options are not defined!");

            String startCommentMarker = rewriter.options.startCommentMarker();
            String endCommentMarker = rewriter.options.endCommentMarker();
            Preconditions.checkArgument(startCommentMarkers.add(startCommentMarker), "Duplicate start comment marker are not allowed, conflict on %s", startCommentMarker);
            Preconditions.checkArgument(endCommentMarkers.add(endCommentMarker), "Duplicate end comment marker are not allowed, conflict on %s", endCommentMarker);
        }

        return new CompositeRewriter(Set.copyOf(rewriters));
    }

    @Override
    public Set<SearchReplaceRewriter> getRewriters() {
        return this.rewriters;
    }
}
