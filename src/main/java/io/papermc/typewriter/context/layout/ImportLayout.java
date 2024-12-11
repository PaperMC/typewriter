package io.papermc.typewriter.context.layout;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * An import layout represents how import are sorted and written
 * inside a java source file.
 * <p>
 * The default layout is defined as: imports -> newline -> static imports
 * (equivalent to {@code *, |, $*} in a .editorconfig using IntelliJ's format)
 * sorted depending on {@link ImportOrder#MERGED}.
 *
 * @see #builder()
 * @see #uniform(String, ImportHeader)
 */
public class ImportLayout {

    /**
     * The default format as defined above in {@link ImportLayout}
     */
    public static final ImportLayout DEFAULT = ImportLayout.uniform("*.java", ImportHeader.DEFAULT);

    private final Map<LazyBaseNameGlob, ImportHeader> headers;

    private ImportLayout(Map<LazyBaseNameGlob, ImportHeader> headers) {
        this.headers = headers;
    }

    /**
     * Gets the relevant order and setup of the imports for
     * the specified path. In cases some matchers overlaps,
     * the priority is given to the last matcher.
     *
     * @param path the target path
     * @return the relevant import header or the default one if no one
     * matches
     */
    public ImportHeader getRelevantHeader(Path path, ImportHeader def) {
        ImportHeader result = def;
        for (Map.Entry<LazyBaseNameGlob, ImportHeader> entry : this.headers.entrySet()) {
            if (entry.getKey().matches(path)) {
                result = entry.getValue();
            }
        }
        return result;
    }

    public static ImportLayout uniform(String glob, ImportHeader header) {
        return new ImportLayout(Map.of(new LazyBaseNameGlob(glob), header));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<LazyBaseNameGlob, ImportHeader> headers = new LinkedHashMap<>();

        /**
         * Define a new import layout for a specific set of files, the file
         * glob pattern must be valid as defined in {@link java.nio.file.FileSystem#getPathMatcher(String)}
         * for the {@code glob:} syntax.
         * Priority of the matches when glob overlaps depends on the caller order.
         *
         * @param fileGlob the glob pattern
         * @param scheme the package filters
         * @param order the import order
         * @return the builder, for chaining
         * @see #getRelevantHeader(Path, ImportHeader)
         */
        public Builder describeLayout(String fileGlob, UnaryOperator<ImportScheme> scheme, ImportOrder order) {
            this.headers.put(new LazyBaseNameGlob(fileGlob), new ImportHeader(scheme.apply(new ImportScheme()), order));
            return this;
        }

        public ImportLayout build() {
            return new ImportLayout(this.headers);
        }
    }

    private static final class LazyBaseNameGlob {

        private final String glob;
        private boolean baseNameOnly; // see https://github.com/editorconfig/editorconfig/issues/283 .editorconfig matches against the file name when no path separator is found

        LazyBaseNameGlob(String glob) {
            this.glob = glob;
        }

        private PathMatcher resolved;

        public PathMatcher resolve() {
            if (this.resolved == null) {
                this.resolved = FileSystems.getDefault().getPathMatcher("glob:" + this.glob);
                this.baseNameOnly = this.glob.indexOf('/') == -1;
            }
            return this.resolved;
        }

        public boolean matches(Path path) {
            PathMatcher matcher = this.resolve();
            Path target = this.baseNameOnly ? path.getFileName() : path;
            return matcher.matches(target);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this || o.getClass() != this.getClass()) {
                return true;
            }

            return this.glob.equals(((LazyBaseNameGlob) o).glob);
        }

        @Override
        public int hashCode() {
            return this.glob.hashCode();
        }
    }
}
