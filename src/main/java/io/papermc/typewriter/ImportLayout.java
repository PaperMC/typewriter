package io.papermc.typewriter;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.context.ImportTypeName;
import io.papermc.typewriter.parser.StringReader;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * An import layout represents how import are sorted and written
 * inside a java source file.
 * <p>
 * The default layout is defined as: imports -> newline -> static imports
 * (equivalent to {@code *, |, $*} in a .editorconfig using IntelliJ's format)
 * <p>
 * Custom layout can be defined either manually using the {@link #builder()}
 * or transferred from a {@code .editorconfig}.
 */
public class ImportLayout {

    /**
     * The default format as defined above in {@link ImportLayout}
     */
    public static final ImportLayout DEFAULT = ImportLayout.builder()
        .addSection("*.java", Section.DEFAULT)
        .build();

    private final Map<LazyBaseNameGlob, Section> sections;

    private ImportLayout(Map<LazyBaseNameGlob, Section> sections) {
        this.sections = sections;
    }

    /**
     * Gets the relevant order and setup of the imports for
     * the specified path. In cases some matchers overlaps,
     * the priority is given to the last matcher.
     *
     * @param path the target path
     * @return the relevant section or the default one if no one
     * matches
     */
    public Section getRelevantSection(Path path) {
        Section result = Section.DEFAULT;
        for (Map.Entry<LazyBaseNameGlob, Section> entry : this.sections.entrySet()) {
            if (entry.getKey().matches(path)) {
                result = entry.getValue();
            }
        }
        return result;
    }

    /**
     * Consumes a {@code .editorconfig} file into an {@link ImportLayout}.
     * The property {@code ij_java_imports_layout} is read to determine the layout
     * and the order is defaulted to {@link Order#MERGED}.
     * <p>
     * This method is experimental since it doesn't take in account conflict with parent configuration.
     *
     * @param file the file
     * @return the import layout
     * @throws IOException If an I/O error occurred while reading the file
     */
    @ApiStatus.Experimental
    public static ImportLayout createFromEditorConfig(Path file) throws IOException {
        Preconditions.checkArgument(file.getFileName().toString().equals(".editorconfig"), "Bad file name");

        Builder builder = ImportLayout.builder();
        String sectionName = null;
        for (String line : Files.readAllLines(file)) {
            if (line.isEmpty()) {
                continue;
            }

            StringReader reader = new StringReader(line);
            reader.skipWhitespace();

            if (reader.canRead() && reader.peek() == '[') {
                reader.skip();
                sectionName = reader.readUntil(']');
                reader.skip();
                reader.skipWhitespace();
            }

            if (sectionName != null && reader.trySkipString("ij_java_imports_layout")) {
                reader.skipWhitespace();
                if (reader.canRead() && reader.peek() == '=') {
                    reader.skip();
                    reader.skipWhitespace();

                    String[] value = reader.getRemaining().split(",");
                    StringBuilder normalizedValue = new StringBuilder();
                    for (int i = 0; i < value.length; i++) {
                        if (i != 0) {
                            normalizedValue.append(',');
                        }
                        normalizedValue.append(value[i].trim());
                    }

                    builder.addSection(sectionName, new Section(normalizedValue.toString(), Order.MERGED));
                }
            }
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<LazyBaseNameGlob, Section> sections = new LinkedHashMap<>();

        /**
         * Add a section into this import layout, the glob pattern must be valid
         * for a {@link PathMatcher}. In a {@code .editorconfig} this method is the equivalent of
         * <pre>
         * [glob]
         * ij_java_imports_layout = [layout]
         * </pre>
         *
         * @param glob the glob pattern
         * @param section the section order and layout
         * @return the builder, for chaining
         */
        public Builder addSection(String glob, Section section) {
            this.sections.put(new LazyBaseNameGlob(glob), section);
            return this;
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

    public record Section(String layout, Order order) {
        public static final Section DEFAULT = new Section("*,|,$*", Order.MERGED);

        private static final Map<String, Predicate<ImportTypeName>> SPECIAL_LAYOUT_DATA = Map.of(
            "*", type -> !type.isStatic(),
            "$*", ImportTypeName::isStatic
        );

        private void sort(Predicate<ImportTypeName> matcher, StringBuilder builder, List<ImportTypeName> types) {
            types.stream().filter(matcher).sorted(this.order.comparator).forEach(type -> writeImport(builder, type));
        }

        // todo support the whole format? and generalize .editorconfig handling for indent unit
        private void translateSymbol(String symbol, StringBuilder builder, List<ImportTypeName> types) {
            if (SPECIAL_LAYOUT_DATA.containsKey(symbol)) {
                this.sort(SPECIAL_LAYOUT_DATA.get(symbol), builder, types);
            } else if (symbol.endsWith(".**")) {
                this.sort(type -> type.name().startsWith(symbol.substring(0, symbol.length() - 2)), builder, types);
            } else {
                throw new UnsupportedOperationException("Cannot handle symbol " + symbol);
            }
        }

        private static void writeImport(StringBuilder builder, ImportTypeName type) {
            builder.append("import ");
            if (type.isStatic()) {
                builder.append("static ");
            }
            builder.append(type.name());
            if (type.isGlobal()) {
                builder.append(".*");
            }
            builder.append(';');
            builder.append('\n');
        }

        public void sortImportsInto(StringBuilder builder, List<ImportTypeName> addedImports) {
            int newlines = 0;
            for (String symbol : this.layout().split(",")) {
                if (symbol.equals("|")) {
                    newlines++;
                    continue;
                }

                int pos = builder.length();
                this.translateSymbol(symbol, builder, addedImports);
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
        GENERATED_FIRST(Comparator.comparing(ImportTypeName::newlyAdded).reversed().thenComparing(Comparator.naturalOrder())),

        /**
         * Generated imports will be written last after the original imports in the class.
         * Each part are ordered alphabetically independently.
         */
        GENERATED_LAST(Comparator.comparing(ImportTypeName::newlyAdded).thenComparing(Comparator.naturalOrder()));

        private final Comparator<ImportTypeName> comparator;

        Order(Comparator<ImportTypeName> comparator) {
            this.comparator = comparator;
        }
    }
}
