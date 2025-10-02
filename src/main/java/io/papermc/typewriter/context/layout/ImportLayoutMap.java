package io.papermc.typewriter.context.layout;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An import layout represents how imports are sorted and written
 * inside a java source file.
 * <p>
 * The default layout is defined as: imports -> newline -> static imports
 * (equivalent to {@code *, |, $*} in a .editorconfig using IntelliJ's format)
 * sorted depending on {@link ImportOrder#MERGED}.
 *
 * @see #builder()
 * @see #uniform(String, ImportLayout)
 */
@DefaultQualifier(NonNull.class)
public class ImportLayoutMap {

    /**
     * The default format as defined above in {@link ImportLayoutMap}
     */
    public static final ImportLayoutMap DEFAULT = ImportLayoutMap.uniform("*.java", ImportLayout.DEFAULT);

    private final Map<LazyBaseNameGlob, ImportLayout> layouts;

    private ImportLayoutMap(Map<LazyBaseNameGlob, ImportLayout> layouts) {
        this.layouts = layouts;
    }

    /**
     * Gets the relevant order and setup of the imports for
     * the specified {@code path}. In cases some matchers overlaps,
     * the priority is given to the last matcher.
     *
     * @param path the target path
     * @return the relevant import layout or the default one if no one
     * matches
     */
    public ImportLayout getRelevantLayout(Path path) {
        return this.getRelevantLayout(path, ImportLayout.DEFAULT);
    }

    /**
     * Gets the relevant order and setup of the imports for
     * the specified {@code path}. In cases some matchers overlaps,
     * the priority is given to the last matcher.
     *
     * @param path the target path
     * @return the relevant import layout or the provided {@code fallback} if no one
     * matches
     */
    public ImportLayout getRelevantLayout(Path path, ImportLayout fallback) {
        ImportLayout result = fallback;
        for (Map.Entry<LazyBaseNameGlob, ImportLayout> entry : this.layouts.entrySet()) {
            if (entry.getKey().matches(path)) {
                result = entry.getValue();
            }
        }
        return result;
    }

    public static ImportLayoutMap uniform(String fileGlob, ImportLayout layout) {
        return new ImportLayoutMap(Map.of(new LazyBaseNameGlob(fileGlob), layout));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<LazyBaseNameGlob, ImportLayout> layouts = new LinkedHashMap<>();

        /**
         * Define a new import layout for a specific set of files, the file
         * glob pattern must be valid as defined in {@link java.nio.file.FileSystem#getPathMatcher(String)}
         * for the {@code glob:} syntax.
         * Priority of the matches when glob overlaps depends on the caller order.
         *
         * @param fileGlob the glob pattern
         * @param layout the import layout
         * @return the builder, for chaining
         * @see #getRelevantLayout(Path, ImportLayout)
         */
        public Builder describeLayout(String fileGlob, ImportLayout layout) {
            this.layouts.put(new LazyBaseNameGlob(fileGlob), layout);
            return this;
        }

        public ImportLayoutMap build() {
            return new ImportLayoutMap(this.layouts);
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
