package io.papermc.typewriter.context.layout;

import com.google.common.collect.Lists;
import io.papermc.typewriter.context.ImportName;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * An import scheme defines a strict schema to enforce for the imports of the rewritten classes
 * matching the glob pattern. Call order matters in this builder and the remaining imports without a defined schema
 * will be appended at the end of the import section. The schema only group import statements
 * without any order information attached to it, for group ordering see {@link ImportOrder}.
 */
@DefaultQualifier(NonNull.class)
public class ImportScheme {

    private final List<MutableItem> items = new ArrayList<>();
    private final List<Item> immutableItems = Collections.unmodifiableList(Lists.transform(this.items, MutableItem::toImmutable));
    private int nextSpace = 0;

    List<Item> view() {
        return this.immutableItems;
    }

    /**
     * Adds a single newline past the current cursor.
     *
     * @return the scheme, for chaining
     * @see #newline(int)
     */
    public ImportScheme newline() {
        return this.newline(1);
    }

    /**
     * Adds some newline past the current cursor.
     *
     * @param count the number of newline to add
     * @return the scheme, for chaining
     * @see #newline()
     */
    public ImportScheme newline(int count) {
        this.nextSpace += count;
        return this;
    }

    /**
     * Group the import statements matching the provided filter.
     *
     * @param filter the filter to group
     * @return the scheme, for chaining
     * @see ImportFilter
     */
    public ImportScheme group(Predicate<? super ImportName> filter) {
        MutableItem item = new MutableItem(filter);
        if (this.nextSpace != 0) {
            item.prependSpace(this.nextSpace);
            this.nextSpace = 0;
        }

        this.items.add(item);
        return this;
    }

    public record Item(int previousSpace, Predicate<? super ImportName> filter) {

        public boolean contains(ImportName name) {
            return this.filter.test(name);
        }
    }

    private static class MutableItem {

        private final Predicate<? super ImportName> filter;
        private int previousSpace;

        private MutableItem(Predicate<? super ImportName> tester) {
            this.filter = tester;
        }

        public void prependSpace(int count) {
            this.previousSpace += count;
        }

        public Item toImmutable() {
            return new Item(this.previousSpace, this.filter);
        }
    }
}
