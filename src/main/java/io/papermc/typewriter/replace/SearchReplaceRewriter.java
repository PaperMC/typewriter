package io.papermc.typewriter.replace;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.ClassNamed;
import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.context.ImportCollector;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

/**
 * The main rewriter that search for content delimited by a start and end comment marker
 * to replace with a generated content. Subclasses must override either (or both)
 * {@link #insert(SearchMetadata, StringBuilder)} and {@link #replaceLine(SearchMetadata, StringBuilder)}
 * to implement exact and regular replacement.
 */
@DefaultQualifier(NonNull.class)
public class SearchReplaceRewriter extends SearchReplaceRewriterBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchReplaceRewriter.class);

    protected @MonotonicNonNull String name;
    protected @MonotonicNonNull ReplaceOptions options;

    @Contract(value = "_ -> this", mutates = "this")
    public SearchReplaceRewriter withOptions(ReplaceOptionsLike options) {
        this.options = options.asOptions();
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public SearchReplaceRewriter customName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public ReplaceOptions getOptions() {
        return this.options;
    }

    // only when exactReplacement = false
    @ApiStatus.OverrideOnly
    protected void insert(SearchMetadata metadata, StringBuilder builder) {
        throw new UnsupportedOperationException("This rewriter (" + this.getClass().getCanonicalName() + ") doesn't support removal and insertion!");
    }

    // only when exactReplacement = true
    @ApiStatus.OverrideOnly
    protected void replaceLine(SearchMetadata metadata, StringBuilder builder) {
        throw new UnsupportedOperationException("This rewriter (" + this.getClass().getCanonicalName() + ") doesn't support exact replacement!");
    }

    public boolean hasGeneratedComment() {
        return !this.options.exactReplacement() && this.options.generatedComment().isPresent();
    }

    @Override
    public boolean registerFor(SourceFile file) {
        if (this.options == null) {
            LOGGER.error("Replace options are not defined, skipping the rewriter: {}", this.getName());
            return false;
        }

        this.options.targetClass().ifPresent(targetClass -> {
            Preconditions.checkArgument(targetClass.root().equals(file.mainClass()), "Target class must be a nested class of " + file.mainClass().canonicalName());
        });

        if (this.name == null) {
            this.name = this.options.targetClass().orElse(file.mainClass()).simpleName();
        }
        return true;
    }

    public void dump(SourceFile file, StringBuilder content) {
        content.append("Name : ").append(this.name);

        content.append('\n');
        content.append("Start comment marker : ").append(this.options.startCommentMarker());
        content.append('\n');
        content.append("End comment marker : ").append(this.options.endCommentMarker());
        content.append('\n');
        content.append('\n');

        if (this.options.exactReplacement()) {
            content.append("[This rewriter only works for exact replacement and cannot be dump safely]");
        } else {
            content.append(">".repeat(30));
            content.append('\n');

            this.insert(new SearchMetadata(file, ImportCollector.NO_OP, file.indentUnit().content(), "", -1), content);

            content.append("<".repeat(30));
        }
        content.append('\n');
    }

    @Override
    public Set<SearchReplaceRewriter> getRewriters() {
        return Collections.singleton(this);
    }
}
