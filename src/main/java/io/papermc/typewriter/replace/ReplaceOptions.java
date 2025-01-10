package io.papermc.typewriter.replace;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.ClassNamed;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Contract;

import java.util.Optional;

@DefaultQualifier(NonNull.class)
public record ReplaceOptions(
    String startCommentMarker,
    String endCommentMarker,
    Optional<String> generatedComment,
    boolean exactReplacement,
    boolean multipleOperation,
    Optional<ClassNamed> targetClass
) implements ReplaceOptionsLike {

    public ReplaceOptions {
        Preconditions.checkState(generatedComment.isEmpty() || !exactReplacement, "Generated comment is not compatible with exact replacement");
    }

    /**
     * Specify the replace options for a rewriter.
     *
     * @param startCommentMarker the start comment where the replacement will start to happen
     * @param endCommentMarker   the end comment where the replacement will terminate
     * @return the builder
     * @apiNote single line comment prefix ("// ") is not needed in the comment and
     * cannot be changed here.
     */
    @Contract(value = "_, _ -> new", pure = true)
    public static Builder between(String startCommentMarker, String endCommentMarker) {
        return new Builder(startCommentMarker, endCommentMarker);
    }

    /**
     * Specify the replace options for a rewriter.
     *
     * @param commentMarker the only comment where the replacement will be delimited by (twice)
     * @return the builder
     * @apiNote single line comment prefix ("// ") is not needed in the comment and
     * cannot be changed here.
     */
    @Contract(value = "_ -> new", pure = true)
    public static Builder between(String commentMarker) {
        return between(commentMarker, commentMarker);
    }

    @Override
    public ReplaceOptions asOptions() {
        return this;
    }

    public static class Builder implements ReplaceOptionsLike {

        private final String startCommentMarker;
        private final String endCommentMarker;
        private @Nullable String generatedComment;
        private boolean exactReplacement;
        private boolean multipleOperation;
        private @Nullable ClassNamed targetClass;

        public Builder(String startCommentMarker, String endCommentMarker) {
            Preconditions.checkArgument(!startCommentMarker.isBlank(), "Start comment marker cannot be blank!");
            Preconditions.checkArgument(!endCommentMarker.isBlank(), "End comment marker cannot be blank!");
            this.startCommentMarker = startCommentMarker;
            this.endCommentMarker = endCommentMarker;
        }

        /**
         * Sets the generated comment (if needed)
         * to print a line after the start comment marker.
         * This is generally used to show in which version your data
         * has been pulled if those are version dependant.
         *
         * @param generatedComment the generated comment
         * @return the builder, for chaining
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder generatedComment(String generatedComment) {
            Preconditions.checkArgument(!generatedComment.isBlank(), "Generated comment cannot be blank!");
            this.generatedComment = generatedComment;
            return this;
        }

        /**
         * Sets if the replacement should be exact.
         * Exact replacement means that for a line removed,
         * a new line will be inserted with your generated content.
         * The callback {@link SearchReplaceRewriter#replaceLine(SearchMetadata, StringBuilder)}
         * will fire for each line replaced. Unlike regular (default)
         * replacement that call {@link SearchReplaceRewriter#insert(SearchMetadata, StringBuilder)}
         * once the enclosing content is removed and the end comment marker is
         * reached.
         *
         * @return the builder, for chaining
         * @apiNote generated comment is not compatible with exact
         * replacement
         */
        @Contract(value = "-> this", mutates = "this")
        public Builder exactReplacement() {
            this.exactReplacement = true;
            return this;
        }

        /**
         * By default, replacement only happens once per file,
         * this option allow multiple replacement using the same
         * comment markers for a single file.
         *
         * @return the builder, for chaining
         */
        @Contract(value = "-> this", mutates = "this")
        public Builder multipleOperation() {
            this.multipleOperation = true;
            return this;
        }

        /**
         * Provides the nested class targeted (if any) as extra context
         * for rewriter.
         *
         * @param nestedClass the nested class
         * @return the builder, for chaining
         */
        @Contract(value = "_ -> this", mutates = "this")
        public Builder targetClass(@Nullable ClassNamed nestedClass) {
            this.targetClass = nestedClass;
            return this;
        }

        @Override
        public ReplaceOptions asOptions() {
            return new ReplaceOptions(
                this.startCommentMarker,
                this.endCommentMarker,
                Optional.ofNullable(this.generatedComment),
                this.exactReplacement,
                this.multipleOperation,
                Optional.ofNullable(this.targetClass)
            );
        }
    }
}
