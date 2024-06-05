package io.papermc.typewriter.preset;

import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.replace.SearchMetadata;
import io.papermc.typewriter.replace.SearchReplaceRewriter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import java.util.Iterator;

public abstract class SwitchCaseRewriter extends SearchReplaceRewriter {

    @MonotonicNonNull
    private Iterator<String> cases;

    protected abstract Iterable<String> getCases();

    @Override
    public boolean registerFor(SourceFile file) {
        boolean canRegister = super.registerFor(file);
        if (canRegister) {
            this.cases = this.getCases().iterator();
        }
        return canRegister;
    }

    private void appendCase(StringBuilder builder, SearchMetadata metadata) {
        builder.append(metadata.indent()).append("case ").append(this.cases.next()).append(':');
        builder.append('\n');
    }

    @Override
    protected void replaceLine(final SearchMetadata metadata, final StringBuilder builder) {
        appendCase(builder, metadata);
    }

    @Override
    protected void insert(final SearchMetadata metadata, final StringBuilder builder) {
        while (this.cases.hasNext()) {
            appendCase(builder, metadata);
        }
    }
}
