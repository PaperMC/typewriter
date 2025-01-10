package io.papermc.typewriter.context;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.Contract;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * File metadata for all source files registered through a {@link io.papermc.typewriter.registration.SourceSetRewriter}.
 *
 * @param importLayout the import layout used
 * @param indentUnit the default indent unit used if no one is found in the source file
 */
@DefaultQualifier(NonNull.class)
public record SourcesMetadata(ImportLayout importLayout, IndentUnit indentUnit, Set<Path> classpath) {

    /**
     * Constructs a file metadata with the default import layout and
     * a specified indent unit.
     *
     * @param indentUnit the indent unit
     * @return the file metadata
     */
    public static SourcesMetadata of(IndentUnit indentUnit, UnaryOperator<Builder> builder) {
        return builder.apply(new Builder(indentUnit)).complete();
    }

    public static SourcesMetadata of(IndentUnit indentUnit) {
        return of(indentUnit, UnaryOperator.identity());
    }

    public static class Builder {

        private final IndentUnit indentUnit;
        private ImportLayout layout = ImportLayout.DEFAULT;
        private Set<Path> classpath = Collections.emptySet();

        Builder(IndentUnit indentUnit) {
            this.indentUnit = indentUnit;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder layout(ImportLayout layout) {
            this.layout = layout;
            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public Builder classpath(Set<Path> classpath) {
            this.classpath = Set.copyOf(classpath);
            return this;
        }

        SourcesMetadata complete() {
            return new SourcesMetadata(
                this.layout,
                this.indentUnit,
                this.classpath
            );
        }
    }
}
