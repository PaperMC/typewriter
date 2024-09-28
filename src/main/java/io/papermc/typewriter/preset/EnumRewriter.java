package io.papermc.typewriter.preset;

import com.google.common.base.Preconditions;
import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.preset.model.EnumValue;
import io.papermc.typewriter.replace.SearchMetadata;
import io.papermc.typewriter.replace.SearchReplaceRewriter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Contract;

import java.util.Iterator;
import java.util.Objects;

public abstract class EnumRewriter<T> extends SearchReplaceRewriter {

    private @MonotonicNonNull Iterator<T> values;
    protected @Nullable Boolean reachEnd;

    @Contract(value = "_ -> this", mutates = "this")
    public EnumRewriter<T> reachEnd(boolean reachEnd) {
        this.reachEnd = reachEnd;
        return this;
    }

    protected abstract Iterable<T> getValues();

    protected abstract EnumValue.Builder rewriteEnumValue(T item);

    protected void appendEnumValue(T item, StringBuilder builder, String indent, boolean reachEnd) {
        this.rewriteEnumValue(item).build().emitCode(indent, this.indentUnit(), builder);
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

    private boolean canReachEnd(SearchMetadata metadata) {
        // the default behavior might fail for simple enum or with trailing comments that's why a setting exists
        return Objects.requireNonNullElseGet(this.reachEnd, () -> metadata.replacedContent().stripTrailing().endsWith(";"));
    }

    @Override
    protected void replaceLine(SearchMetadata metadata, StringBuilder builder) {
        Preconditions.checkState(this.values.hasNext(), "Enum size doesn't match between generated values and replaced values.");
        appendEnumValue(this.values.next(), builder, metadata.indent(), this.canReachEnd(metadata));
    }

    @Override
    protected void insert(SearchMetadata metadata, StringBuilder builder) {
        boolean reachEnd = this.canReachEnd(metadata);

        while (this.values.hasNext()) {
            appendEnumValue(this.values.next(), builder, metadata.indent(), reachEnd);
        }
    }
}
