package io.papermc.typewriter.preset;

import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.replace.SearchMetadata;
import io.papermc.typewriter.replace.SearchReplaceRewriter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.Iterator;

public abstract class EnumRewriter<T> extends SearchReplaceRewriter {

    @MonotonicNonNull
    private Iterator<T> values;

    protected abstract Iterable<T> getValues();

    protected abstract String rewriteEnumName(T item);

    protected @Nullable String rewriteEnumValue(T item) {
        return null;
    }

    protected void rewriteAnnotation(T item, StringBuilder builder, SearchMetadata metadata) {}

    private void appendEnumValue(StringBuilder builder, SearchMetadata metadata, boolean reachEnd) {
        T item = this.values.next();

        this.rewriteAnnotation(item, builder, metadata);

        builder.append(metadata.indent()).append(this.rewriteEnumName(item));
        @Nullable String value = this.rewriteEnumValue(item);
        if (value != null) {
            builder.append('(').append(value).append(')');
        }
        if (reachEnd && !this.values.hasNext()) {
            builder.append(';');
        } else {
            builder.append(',');
        }
        builder.append('\n');
    }

    @Override
    public boolean registerFor(SourceFile file) {
        boolean canRegister = super.registerFor(file);
        if (canRegister) {
            this.values = this.getValues().iterator();
        }
        return canRegister;
    }

    @Override
    protected void replaceLine(final SearchMetadata metadata, final StringBuilder builder) {
        // todo (softspoon, after cleanup): appendEnumValue(builder, metadata, metadata.replacedContent().stripTrailing().endsWith(";"));
        appendEnumValue(builder, metadata, metadata.replacedContent().stripTrailing().lastIndexOf(';') != -1);
    }

    @Override
    protected void insert(final SearchMetadata metadata, final StringBuilder builder) {
        // todo (softspoon, after cleanup): boolean reachEnd = metadata.replacedContent().stripTrailing().endsWith(";");
        String replacedContent = metadata.replacedContent().stripTrailing(); // ignore last new line char
        String lastLine = replacedContent.substring(replacedContent.lastIndexOf('\n') + 1);
        boolean reachEnd = lastLine.lastIndexOf(';') != -1;
        // this is super lenient but shouldn't really appear in the api comments/string anyway (handle trailing comments like in ItemRarity after semi colon)

        while (this.values.hasNext()) {
            appendEnumValue(builder, metadata, reachEnd);
        }
    }
}
