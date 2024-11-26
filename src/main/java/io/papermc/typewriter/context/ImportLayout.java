package io.papermc.typewriter.context;

import com.google.common.base.Preconditions;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * An import layout represents how import are sorted and written
 * inside a java source file.
 * <p>
 * The default layout is defined as: imports -> newline -> static imports
 * (equivalent to {@code *, |, $*} in a .editorconfig using IntelliJ's format)
 * sorted depending on {@link Order#MERGED}.
 *
 * @see #builder()
 * @see #layout(String, Header)
 */
public class ImportLayout {

    /**
     * The default format as defined above in {@link ImportLayout}
     */
    public static final ImportLayout DEFAULT = ImportLayout.layout("*.java", Header.DEFAULT);

    private final Map<LazyBaseNameGlob, Header> headers;

    private ImportLayout(Map<LazyBaseNameGlob, Header> headers) {
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
    public Header getRelevantHeader(Path path, Header def) {
        Header result = def;
        for (Map.Entry<LazyBaseNameGlob, Header> entry : this.headers.entrySet()) {
            if (entry.getKey().matches(path)) {
                result = entry.getValue();
            }
        }
        return result;
    }

    public static ImportLayout layout(String glob, Header header) {
        return new ImportLayout(Map.of(new LazyBaseNameGlob(glob), header));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<LazyBaseNameGlob, Header> sections = new LinkedHashMap<>();

        /**
         * Add a section into this import layout, the file glob pattern must be valid
         * for a {@link PathMatcher}.
         * Priority of the matches when glob overlaps depends on the caller order.
         *
         * @param fileGlob the glob pattern
         * @param sections the import sections of the affected files
         * @param order the import order in that section
         * @return the builder, for chaining
         * @see #getRelevantHeader(Path, Header)
         */
        public Builder addSection(String fileGlob, List<Section> sections, Order order) {
            this.sections.put(new LazyBaseNameGlob(fileGlob), new Header(List.copyOf(sections), order));
            return this;
        }

        /**
         * Add a section into this import layout, the file glob pattern must be valid
         * for a {@link PathMatcher}.
         * Priority of the matches when glob overlaps depends on the caller order.
         *
         * @param fileGlob the glob pattern
         * @param sections the package filters
         * @param order the import order
         * @return the builder, for chaining
         * @see #getRelevantHeader(Path, Header)
         */
        public Builder addSection(String fileGlob, UnaryOperator<List<Section>> sections, Order order) {
            return this.addSection(fileGlob, List.copyOf(sections.apply(new ArrayList<>())), order);
        }

        public ImportLayout build() {
            return new ImportLayout(this.sections);
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

    public interface PackageFilter extends Section {

        static PackageFilter wrap(Predicate<ImportName> delegate) {
            return delegate::test;
        }

        boolean test(ImportName name);
    }

    public interface Section {

    }

    public record BlankLine(int count) implements Section {
        public static BlankLine times(int count) {
            Preconditions.checkArgument(count > 0, "Negative amount is not allowed");
            return new BlankLine(count);
        }

        public static BlankLine single() {
            return times(1);
        }
    }

    public record Header(List<Section> sections, Order order) {

        public static final Header DEFAULT = new Header(List.of(
            ImportCategory.TYPE.allOther(),
            BlankLine.single(),
            ImportCategory.STATIC.allOther()
        ), Order.MERGED);

        private static void writeImport(StringBuilder builder, ImportName type) {
            builder.append("import ");
            if (type instanceof ImportName.Static) {
                builder.append("static ");
            }
            builder.append(type.name());
            builder.append(';');
            builder.append('\n');
        }

        void sortImportsInto(StringBuilder builder, List<ImportName> addedImports) {
            int newlines = 0;
            for (Section section : this.sections) {
                if (section instanceof BlankLine bl) {
                    newlines += bl.count();
                    continue;
                }

                int pos = builder.length();

                addedImports.stream()
                    .filter(((PackageFilter) section)::test)
                    .sorted(this.order.comparator)
                    .forEach(type -> writeImport(builder, type));

                if (newlines > 0 && pos != builder.length()) { // only insert the newlines if there's imports remaining
                    builder.insert(pos, "\n".repeat(newlines));
                }

                newlines = 0;
            }

            builder.deleteCharAt(builder.length() - 1); // remove last new line...
        }
    }

    public enum Order {

        /**
         * Imports will be merged and sorted alphabetically uniformly.
         */
        MERGED(Comparator.naturalOrder()),

        /**
         * Generated imports will be written first followed by the original imports in the class.
         * Each part are ordered alphabetically independently.
         */
        GENERATED_FIRST(Comparator.comparing(ImportName::newlyAdded).reversed().thenComparing(Comparator.naturalOrder())),

        /**
         * Generated imports will be written last after the original imports in the class.
         * Each part are ordered alphabetically independently.
         */
        GENERATED_LAST(Comparator.comparing(ImportName::newlyAdded).thenComparing(Comparator.naturalOrder()));

        private final Comparator<ImportName> comparator;

        Order(Comparator<ImportName> comparator) {
            this.comparator = comparator;
        }
    }
}
