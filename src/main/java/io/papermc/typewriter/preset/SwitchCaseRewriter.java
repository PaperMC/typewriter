package io.papermc.typewriter.preset;

import io.papermc.typewriter.SourceFile;
import io.papermc.typewriter.replace.SearchMetadata;
import io.papermc.typewriter.replace.SearchReplaceRewriter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Iterator;

public abstract class SwitchCaseRewriter extends SearchReplaceRewriter {

    private @MonotonicNonNull Iterator<String> cases;

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
    protected void replaceLine(SearchMetadata metadata, StringBuilder builder) {
        appendCase(builder, metadata);
    }

    @Override
    protected void insert(SearchMetadata metadata, StringBuilder builder) {
        while (this.cases.hasNext()) {
            appendCase(builder, metadata);
        }
    }
}
