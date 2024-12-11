package io.papermc.typewriter.context.layout;

import com.google.common.collect.Lists;
import io.papermc.typewriter.context.ImportName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class ImportScheme {

    private final List<MutableItem> items = new ArrayList<>();
    private final List<Item> frozenItems = Collections.unmodifiableList(Lists.transform(this.items, MutableItem::toImmutable));
    private int nextSpace = 0;

    public final List<Item> view() {
        return this.frozenItems;
    }

    public ImportScheme newline() {
        return this.newline(1);
    }

    public ImportScheme newline(int count) {
        this.nextSpace += count;
        return this;
    }

    public ImportScheme group(Predicate<? super ImportName> name) {
        MutableItem item = new MutableItem(name);
        if (this.nextSpace != 0) {
            item.prependSpace(this.nextSpace);
            this.nextSpace = 0;
        }

        this.items.add(item);
        return this;
    }

    public ImportScheme group(PackageFilter filter) {
        return this.group((Predicate<? super ImportName>) filter);
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
